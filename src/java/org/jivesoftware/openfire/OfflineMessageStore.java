/**
 * $RCSfile$
 * $Revision: 2911 $
 * $Date: 2005-10-03 12:35:52 -0300 (Mon, 03 Oct 2005) $
 *
 * Copyright (C) 2004-2008 Jive Software. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.openfire;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.io.SAXReader;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.database.SequenceManager;
import org.jivesoftware.openfire.container.BasicModule;
import org.jivesoftware.openfire.event.UserEventDispatcher;
import org.jivesoftware.openfire.event.UserEventListener;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.util.JiveConstants;
import org.jivesoftware.util.LocaleUtils;
import org.jivesoftware.util.PropertyUtil;
import org.jivesoftware.util.StringUtils;
import org.jivesoftware.util.XMPPDateTimeFormat;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents the user's offline message storage. A message store holds messages that were
 * sent to the user while they were unavailable. The user can retrieve their messages by
 * setting their presence to "available". The messages will then be delivered normally.
 * Offline message storage is optional, in which case a null implementation is returned that
 * always throws UnauthorizedException when adding messages to the store.
 *
 * @author Iain Shigeoka
 */
public class OfflineMessageStore extends BasicModule implements UserEventListener {

	private static final Logger Log = LoggerFactory.getLogger(OfflineMessageStore.class);

    private static final String INSERT_OFFLINE =
        "INSERT INTO ofOffline (username, messageID, creationDate, messageSize, stanza) " +
        "VALUES (?, ?, ?, ?, ?)";
    private static final String LOAD_OFFLINE =
        "SELECT stanza, creationDate FROM ofOffline WHERE username=? ORDER BY creationDate ASC";
    private static final String LOAD_OFFLINE_MESSAGE =
        "SELECT stanza FROM ofOffline WHERE username=? AND creationDate=?";
    private static final String SELECT_SIZE_OFFLINE =
        "SELECT SUM(messageSize) FROM ofOffline WHERE username=?";
    private static final String SELECT_SIZE_ALL_OFFLINE =
        "SELECT SUM(messageSize) FROM ofOffline";
    private static final String DELETE_OFFLINE =
        "DELETE FROM ofOffline WHERE username=?";
    private static final String DELETE_OFFLINE_MESSAGE =
        "DELETE FROM ofOffline WHERE username=? AND creationDate=?";
    private static final String JPUSH_URL =
        	"http://192.168.1.145:8080/Gnet_QM_PushService/page/pushController/pushType";
    private static final String JPUSH_PARAMS =
        	"\"USERIDINFO\":{0},\"CONTENT\":\"{1}\",\"PACKAGENAME\":\"{2}\","
        	+ "\"GOTYPE\":\"{3}\"";
    
    private static final int POOL_SIZE = 10;
    
    private Cache<String, Integer> sizeCache;

    /**
     * Pattern to use for detecting invalid XML characters. Invalid XML characters will
     * be removed from the stored offline messages.
     */
    private Pattern pattern = Pattern.compile("&\\#[\\d]+;");

    /**
     * Returns the instance of <tt>OfflineMessageStore</tt> being used by the XMPPServer.
     *
     * @return the instance of <tt>OfflineMessageStore</tt> being used by the XMPPServer.
     */
    public static OfflineMessageStore getInstance() {
        return XMPPServer.getInstance().getOfflineMessageStore();
    }

    /**
     * Pool of SAX Readers. SAXReader is not thread safe so we need to have a pool of readers.
     */
    private BlockingQueue<SAXReader> xmlReaders = new LinkedBlockingQueue<>(POOL_SIZE);

    /**
     * Constructs a new offline message store.
     */
    public OfflineMessageStore() {
        super("Offline Message Store");
        sizeCache = CacheFactory.createCache("Offline Message Size");
    }

    /**
     * Adds a message to this message store. Messages will be stored and made
     * available for later delivery.
     *
     * @param message the message to store.
     */
    public void addMessage(Message message) {
        if (message == null) {
            return;
        }
        if(!shouldStoreMessage(message)) {
            return;
        }
        JID recipient = message.getTo();
        String username = recipient.getNode();
        // If the username is null (such as when an anonymous user), don't store.
        if (username == null || !UserManager.getInstance().isRegisteredUser(recipient)) {
            return;
        }
        else
        if (!XMPPServer.getInstance().getServerInfo().getXMPPDomain().equals(recipient.getDomain())) {
            // Do not store messages sent to users of remote servers
            return;
        }

        long messageID = SequenceManager.nextID(JiveConstants.OFFLINE);

        // Get the message in XML format.
        String msgXML = message.getElement().asXML();

        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(INSERT_OFFLINE);
            pstmt.setString(1, username);
            pstmt.setLong(2, messageID);
            pstmt.setString(3, StringUtils.dateToMillis(new java.util.Date()));
            pstmt.setInt(4, msgXML.length());
            pstmt.setString(5, msgXML);
            pstmt.executeUpdate();
          //解析离校消息字段，获取推送唯一标识及内容
            StringReader read = new StringReader(msgXML);
            InputSource source = new InputSource(read);
            SAXBuilder sb = new SAXBuilder();
            //通过输入源构造一个Document
            Document doc = sb.build(source);
            //取的根元素
            org.jdom.Element root = doc.getRootElement();
            System.out.println(root.getName());//输出根元素的名称（测试）
            System.out.println(root.getAttributeValue("to"));//获取根节点属性值
            System.out.println(root.getAttributeValue("type"));//获取根节点属性值
            org.jdom.Element et = root.getChild("body");//获取body子节点
            System.out.println(et.getText());//获取子节点内容
            String userId = root.getAttributeValue("to");
            String content = et.getText();
            String goType = root.getAttributeValue("type");
            System.out.println(PropertyUtil.getProperty("url")+"this is OfflineMessageStore class ...");
            //推送处理
//            String pushParam = "JSON_PARAM={\"HEAD\":{\"VERSION\":\"\",\"DEVICETYPE\":\"\",\"USERID\":\"\","
//            		+ "\"DRIVERID\":\"\",\"CLOUDID\":\"\",\"SIGN\":\"\",\"ZIP\":\"\"},"
//            		+ "\"INFO\":{"+MessageFormat.format(JPUSH_PARAMS,"[\""+userId+"\"]",content,"com.gnet.enp.ibd",goType)+"}}";
//            BufferedReader reader = null;
//    	    String result = null;
//    	    StringBuffer sbf = new StringBuffer();
//            URL url = new URL(JPUSH_URL);
//	        HttpURLConnection connection = (HttpURLConnection) url
//	                .openConnection();
//	        connection.setRequestMethod("POST");
//	        connection.setDoOutput(true);  
//	        connection.setRequestProperty("User-Agent", "directclient");
//	        connection.connect();
//	        PrintWriter out = new PrintWriter(new OutputStreamWriter(connection.getOutputStream(),"utf-8"));  
//	        out.println(pushParam);  
//	        out.close();
//	        InputStream is = connection.getInputStream();
//	        reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
//	        String strRead = null;
//	        while ((strRead = reader.readLine()) != null) {
//	            sbf.append(strRead);
//	            sbf.append("\r\n");
//	        }
//	        reader.close();
//	        result = sbf.toString();
//	        System.out.println(result);
        }

        catch (Exception e) {
            Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
        }
        finally {
            DbConnectionManager.closeConnection(pstmt, con);
        }

        // Update the cached size if it exists.
        if (sizeCache.containsKey(username)) {
            int size = sizeCache.get(username);
            size += msgXML.length();
            sizeCache.put(username, size);
        }
    }

    /**
     * Returns a Collection of all messages in the store for a user.
     * Messages may be deleted after being selected from the database depending on
     * the delete param.
     *
     * @param username the username of the user who's messages you'd like to receive.
     * @param delete true if the offline messages should be deleted.
     * @return An iterator of packets containing all offline messages.
     */
    public Collection<OfflineMessage> getMessages(String username, boolean delete) {
        List<OfflineMessage> messages = new ArrayList<>();
        SAXReader xmlReader = null;
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            // Get a sax reader from the pool
            xmlReader = xmlReaders.take();
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(LOAD_OFFLINE);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                String msgXML = rs.getString(1);
                Date creationDate = new Date(Long.parseLong(rs.getString(2).trim()));
                OfflineMessage message;
                try {
                    message = new OfflineMessage(creationDate,
                            xmlReader.read(new StringReader(msgXML)).getRootElement());
                } catch (DocumentException e) {
                    // Try again after removing invalid XML chars (e.g. &#12;)
                    Matcher matcher = pattern.matcher(msgXML);
                    if (matcher.find()) {
                        msgXML = matcher.replaceAll("");
                    }
                    try {
                    	message = new OfflineMessage(creationDate,
                            xmlReader.read(new StringReader(msgXML)).getRootElement());
                    } catch (DocumentException de) {
                    	Log.error("Failed to route packet (offline message): " + msgXML, de);
                    	continue; // skip and process remaining offline messages
                    }
                }

                // if there is already a delay stamp, we shouldn't add another.
                Element delaytest = message.getChildElement("delay", "urn:xmpp:delay");
                if (delaytest == null) {
                    // Add a delayed delivery (XEP-0203) element to the message.
                    Element delay = message.addChildElement("delay", "urn:xmpp:delay");
                    delay.addAttribute("from", XMPPServer.getInstance().getServerInfo().getXMPPDomain());
                    delay.addAttribute("stamp", XMPPDateTimeFormat.format(creationDate));
                }
                messages.add(message);
            }
            // Check if the offline messages loaded should be deleted, and that there are
            // messages to delete.
            if (delete && !messages.isEmpty()) {
                PreparedStatement pstmt2 = null;
                try {
                    pstmt2 = con.prepareStatement(DELETE_OFFLINE);
                    pstmt2.setString(1, username);
                    pstmt2.executeUpdate();
                    removeUsernameFromSizeCache(username);
                }
                catch (Exception e) {
                    Log.error("Error deleting offline messages of username: " + username, e);
                }
                finally {
                    DbConnectionManager.closeStatement(pstmt2);
                } 
            }
        }
        catch (Exception e) {
            Log.error("Error retrieving offline messages of username: " + username, e);
        }
        finally {
            DbConnectionManager.closeConnection(rs, pstmt, con);
            // Return the sax reader to the pool
            if (xmlReader != null) {
                xmlReaders.add(xmlReader);
            }
        }
        return messages;
    }

    /**
     * Returns the offline message of the specified user with the given creation date. The
     * returned message will NOT be deleted from the database.
     *
     * @param username the username of the user who's message you'd like to receive.
     * @param creationDate the date when the offline message was stored in the database.
     * @return the offline message of the specified user with the given creation stamp.
     */
    public OfflineMessage getMessage(String username, Date creationDate) {
        OfflineMessage message = null;
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        SAXReader xmlReader = null;
        try {
            // Get a sax reader from the pool
            xmlReader = xmlReaders.take();
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(LOAD_OFFLINE_MESSAGE);
            pstmt.setString(1, username);
            pstmt.setString(2, StringUtils.dateToMillis(creationDate));
            rs = pstmt.executeQuery();
            while (rs.next()) {
                String msgXML = rs.getString(1);
                message = new OfflineMessage(creationDate,
                        xmlReader.read(new StringReader(msgXML)).getRootElement());
                // Add a delayed delivery (XEP-0203) element to the message.
                Element delay = message.addChildElement("delay", "urn:xmpp:delay");
                delay.addAttribute("from", XMPPServer.getInstance().getServerInfo().getXMPPDomain());
                delay.addAttribute("stamp", XMPPDateTimeFormat.format(creationDate));
            }
        }
        catch (Exception e) {
            Log.error("Error retrieving offline messages of username: " + username +
                    " creationDate: " + creationDate, e);
        }
        finally {
            // Return the sax reader to the pool
            if (xmlReader != null) {
                xmlReaders.add(xmlReader);
            }
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }
        return message;
    }

    /**
     * Deletes all offline messages in the store for a user.
     *
     * @param username the username of the user who's messages are going to be deleted.
     */
    public void deleteMessages(String username) {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(DELETE_OFFLINE);
            pstmt.setString(1, username);
            pstmt.executeUpdate();
            
            removeUsernameFromSizeCache(username);
        }
        catch (Exception e) {
            Log.error("Error deleting offline messages of username: " + username, e);
        }
        finally {
            DbConnectionManager.closeConnection(pstmt, con);
        }
    }

    private void removeUsernameFromSizeCache(String username) {
        // Update the cached size if it exists.
        if (sizeCache.containsKey(username)) {
            sizeCache.remove(username);
        }
    }

    /**
     * Deletes the specified offline message in the store for a user. The way to identify the
     * message to delete is based on the creationDate and username.
     *
     * @param username the username of the user who's message is going to be deleted.
     * @param creationDate the date when the offline message was stored in the database.
     */
    public void deleteMessage(String username, Date creationDate) {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(DELETE_OFFLINE_MESSAGE);
            pstmt.setString(1, username);
            pstmt.setString(2, StringUtils.dateToMillis(creationDate));
            pstmt.executeUpdate();
            
            // Force a refresh for next call to getSize(username),
            // it's easier than loading the message to be deleted just
            // to update the cache.
            removeUsernameFromSizeCache(username);
        }
        catch (Exception e) {
            Log.error("Error deleting offline messages of username: " + username +
                    " creationDate: " + creationDate, e);
        }
        finally {
            DbConnectionManager.closeConnection(pstmt, con);
        }
    }

    /**
     * Returns the approximate size (in bytes) of the XML messages stored for
     * a particular user.
     *
     * @param username the username of the user.
     * @return the approximate size of stored messages (in bytes).
     */
    public int getSize(String username) {
        // See if the size is cached.
        if (sizeCache.containsKey(username)) {
            return sizeCache.get(username);
        }
        int size = 0;
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(SELECT_SIZE_OFFLINE);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                size = rs.getInt(1);
            }
            // Add the value to cache.
            sizeCache.put(username, size);
        }
        catch (Exception e) {
            Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
        }
        finally {
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }
        return size;
    }

    /**
     * Returns the approximate size (in bytes) of the XML messages stored for all
     * users.
     *
     * @return the approximate size of all stored messages (in bytes).
     */
    public int getSize() {
        int size = 0;
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(SELECT_SIZE_ALL_OFFLINE);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                size = rs.getInt(1);
            }
        }
        catch (Exception e) {
            Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
        }
        finally {
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }
        return size;
    }

    @Override
    public void userCreated(User user, Map params) {
        //Do nothing
    }

    @Override
    public void userDeleting(User user, Map params) {
        // Delete all offline messages of the user
        deleteMessages(user.getUsername());
    }

    @Override
    public void userModified(User user, Map params) {
        //Do nothing
    }

    @Override
	public void start() throws IllegalStateException {
        super.start();
        // Initialize the pool of sax readers
        for (int i=0; i<POOL_SIZE; i++) {
            SAXReader xmlReader = new SAXReader();
            xmlReader.setEncoding("UTF-8");
            xmlReaders.add(xmlReader);
        }
        // Add this module as a user event listener so we can delete
        // all offline messages when a user is deleted
        UserEventDispatcher.addListener(this);
    }

    @Override
	public void stop() {
        super.stop();
        // Clean up the pool of sax readers
        xmlReaders.clear();
        // Remove this module as a user event listener
        UserEventDispatcher.removeListener(this);
    }

    /**
     * Decide whether a message should be stored offline according to XEP-0160 and XEP-0334.
     *
     * @param message
     * @return <code>true</code> if the message should be stored offline, <code>false</code> otherwise.
     */
    static boolean shouldStoreMessage(final Message message) {
        // XEP-0334: Implement the <no-store/> hint to override offline storage
        if (message.getChildElement("no-store", "urn:xmpp:hints") != null) {
            return false;
        }

        switch (message.getType()) {
            case chat:
                // XEP-0160: Messages with a 'type' attribute whose value is "chat" SHOULD be stored offline, with the exception of messages that contain only Chat State Notifications (XEP-0085) [7] content

                // Iterate through the child elements to see if we can find anything that's not a chat state notification or
                // real time text notification
                Iterator<?> it = message.getElement().elementIterator();

                while (it.hasNext()) {
                    Object item = it.next();

                    if (item instanceof Element) {
                        Element el = (Element) item;
                        if (Namespace.NO_NAMESPACE.equals(el.getNamespace())) {
                            continue;
                        }
                        if (!el.getNamespaceURI().equals("http://jabber.org/protocol/chatstates")
                                && !(el.getQName().equals(QName.get("rtt", "urn:xmpp:rtt:0")))
                                ) {
                            return true;
                        }
                    }
                }

                return message.getBody() != null && !message.getBody().isEmpty();

            case groupchat:
            case headline:
                // XEP-0160: "groupchat" message types SHOULD NOT be stored offline
                // XEP-0160: "headline" message types SHOULD NOT be stored offline
                return false;

            case error:
                // XEP-0160: "error" message types SHOULD NOT be stored offline,
                // although a server MAY store advanced message processing errors offline
                if (message.getChildElement("amp", "http://jabber.org/protocol/amp") == null) {
                    return false;
                }
                break;

            default:
                // XEP-0160: Messages with a 'type' attribute whose value is "normal" (or messages with no 'type' attribute) SHOULD be stored offline.
                break;
        }
        return true;
    }
}
