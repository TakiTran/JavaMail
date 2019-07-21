package com.topica.mail;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;

public class CheckingMails {

	static Logger logger = Logger.getLogger(CheckingMails.class.getName());
	private static final int BUFFER = 2048;

	public static void main(String[] args) {

		String host = "imap.gmail.com";
		String username = "daotv97@gmail.com";
		String password = "tranvandao";

		readMail(host, username, password);

	}

	private static Store connect(String host, String user, String password) throws MessagingException {
		final String PROTOCOL = "imaps";
		Properties properties = new Properties();

		properties.put("mail.store.protocol", PROTOCOL);
		properties.put("mail.imap.host", host);
		properties.put("mail.imap.port", "993");
		properties.put("mail.imap.starttls.enable", "true");
		Session emailSession = Session.getDefaultInstance(properties);

		// create the POP3 store object and connect with the pop server
		Store store = emailSession.getStore(PROTOCOL);
		// connect
		store.connect(host, user, password);
		return store;
	}

	public static boolean checkSubject(Message message, String text) {
		boolean check = false;
		try {
			String subject = message.getSubject().trim();
			if (Pattern.matches("^" + text + ".*$", subject)) {
				check = true;
			}
		} catch (MessagingException e) {
			logger.info(e.getMessage());
		}
		return check;
	}

	private static void extract(String source, String dest) {
		try {
			File root = new File(dest);
			if (!root.exists()) {
				root.mkdir();
			}
			BufferedOutputStream bos = null;
			try (FileInputStream fis = new FileInputStream(source)) {
				try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis))) {
					ZipEntry entry;
					while ((entry = zis.getNextEntry()) != null) {
						String fileName = entry.getName();
						File file = new File(dest + File.separator + fileName);
						if (!entry.isDirectory()) {
							extractFileContentFromArchive(file, zis);
						} else {
							if (!file.exists()) {
								file.mkdirs();
							}
						}
					}

				}
			}
		} catch (Exception e) {
			logger.info(e.getMessage());
		}

	}

	private static void extractFileContentFromArchive(File file, ZipInputStream zis) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		try (BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER)) {
			int len = 0;
			byte []data = new byte[BUFFER];
			while ((len = zis.read(data, 0, BUFFER)) != -1) {
				bos.write(data, 0, len);
			}
			bos.flush();
		}
	}

	public static void readMail(String host, String user, String password) {
		String urlOutput = "mail//";
		try {
			String textSubject = "ITLAB-HOMEWORK";
			String typeExteansion = "zip";
			int sizeFile = 1024 * 5;

			Store store = connect(host, user, password);

			// create the folder object and open it
			Folder emailFolder = store.getFolder("INBOX");
			emailFolder.open(Folder.READ_ONLY);

			// retrieve the messages from the folder in an array and print it
			Message[] messages = emailFolder.getMessages();

			for (int i = 0, n = messages.length; i < n; i++) {
				Message message = messages[i];
				if (checkSubject(message, textSubject)) {
					Multipart multipart = (Multipart) message.getContent();
					for (int j = 0; j < multipart.getCount(); j++) {
						BodyPart bodyPart = multipart.getBodyPart(j);
						if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
							Address[] froms = message.getFrom();
							String nameSender = froms == null ? null : ((InternetAddress) froms[0]).getAddress();
							String fileName = bodyPart.getFileName();
							int lastIndexDot = fileName.lastIndexOf('.');
							if (typeExteansion.equals(fileName.substring(lastIndexDot + 1))) {
								if (nameSender != null) {
									nameSender = nameSender.concat("-").concat(fileName);
								} else {
									nameSender = "Anonymous".concat("-").concat(fileName);
								}
								try (FileOutputStream fileOutputStream = new FileOutputStream(urlOutput.concat(nameSender))) {
									InputStream inputStream = bodyPart.getInputStream();
									byte[] buffer = new byte[sizeFile];
									int bytesRead;
									while ((bytesRead = inputStream.read(buffer)) != -1) {
										fileOutputStream.write(buffer, 0, bytesRead);
									}
								}
								extract(urlOutput.concat(nameSender),urlOutput.concat(nameSender).substring(0, nameSender.lastIndexOf('.')));

							} 
						}

					}
				}
			}

			// close the store and folder objects
			emailFolder.close(false);
			store.close();

		} catch (Exception e) {
			logger.info(e.getMessage());
		}
	}

}

