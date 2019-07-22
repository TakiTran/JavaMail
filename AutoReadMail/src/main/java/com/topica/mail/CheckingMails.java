package com.topica.mail;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

import com.topica.util.CodeExecutor;

public class CheckingMails {

	static Logger logger = Logger.getLogger(CheckingMails.class.getName());
	private static final int BUFFER = 2048;
	private static final String MESSAGE_SUCCESS = "Send result to ";
	private static final String MESSAGE_ERROR = "Send error to ";
	
	public static void main(String[] args) {

		String host = "imap.gmail.com";
		String username = "daotv97@gmail.com";
		String pass = "tranvandao";

		readMail(host, username, pass);

	}

	private static Store connect(String host, String user, String pass) throws MessagingException {
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
		store.connect(host, user, pass);
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
			byte[] data = new byte[BUFFER];
			while ((len = zis.read(data, 0, BUFFER)) != -1) {
				bos.write(data, 0, len);
			}
			bos.flush();
		}
	}

	public static void readMail(String host, String user, String pass) {
		String urlOutput = "./output";
		String textSubject = "ITLAB-HOMEWORK";
		String textResult = "EXERCISE POINT";
		String typeExteansion = "zip";
		int sizeFile = 1024 * 5;
		File outputFile = new File(urlOutput);
		if (!outputFile.exists()) {
			outputFile.mkdirs();
		}
		try {

			Store store = connect(host, user, pass);

			// create the folder object and open it
			Folder emailFolder = store.getFolder("INBOX");
			emailFolder.open(Folder.READ_ONLY);

			// retrieve the messages from the folder in an array and print it
			Message[] messages = emailFolder.getMessages();

			for (int i = 0, n = messages.length; i < n; i++) {
				Message message = messages[i];
				if (checkSubject(message, textSubject)) {
					Multipart multipart = (Multipart) message.getContent();
					Address[] froms = message.getFrom();
					String nameSender = froms == null ? null : ((InternetAddress) froms[0]).getAddress();
					String messageResult = "";
					for (int j = 0; j < multipart.getCount(); j++) {
						BodyPart bodyPart = multipart.getBodyPart(j);
						if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
							String fileName = bodyPart.getFileName();
							int lastIndexDot = fileName.lastIndexOf('.');
							if (typeExteansion.equals(fileName.substring(lastIndexDot + 1))) {
								File outputFileSub = new File(urlOutput.concat("\\").concat(nameSender));
								if (!outputFileSub.exists()) {
									outputFileSub.mkdirs();
								}
								try (FileOutputStream fileOutputStream = new FileOutputStream(
										outputFileSub.getPath().concat("\\").concat(fileName))) {
									InputStream inputStream = bodyPart.getInputStream();
									byte[] buffer = new byte[sizeFile];
									int bytesRead;
									while ((bytesRead = inputStream.read(buffer)) != -1) {
										fileOutputStream.write(buffer, 0, bytesRead);
									}
								}
								// extract file zip.
								extract(outputFileSub.getPath().concat("\\").concat(fileName), outputFileSub.getPath());
								CodeExecutor worker = new CodeExecutor(outputFileSub.getPath().concat("\\").concat((fileName).substring(0,fileName.lastIndexOf('.')).concat(".java")));
								ExecutorService executorService = Executors.newScheduledThreadPool(5);
								Future<Integer> result =  executorService.submit(worker);
								int grade = result.get();
								SendingMails.sendEmail(user, pass, nameSender, textResult, grade +"/10");
								messageResult = MESSAGE_SUCCESS.concat(nameSender);
								logger.info(messageResult);
							} else {
								SendingMails.sendEmail(user, pass, nameSender, textResult, "Attachment extansion in your message not *.zip");
								logger.info("Not file.zip");
								messageResult = MESSAGE_ERROR.concat(nameSender);
								logger.info(messageResult);
							}
						} else {
							SendingMails.sendEmail(user, pass, nameSender, textResult, "Your message does not have an attachment.");
							messageResult = MESSAGE_ERROR.concat(nameSender);
							logger.info(messageResult);
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
