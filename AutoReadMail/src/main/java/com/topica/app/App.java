package com.topica.app;

import com.topica.mail.CheckingMails;

public class App {
	private static final String HOST = "imap.gmail.com";
	private static final String EMAIL_USERNAME = "daotv97@gmail.com";
	private static final String EMAIL_PASWORD = "tranvandao";

	public static void main(String[] args) {
		CheckingMails.readMail(HOST, EMAIL_USERNAME, EMAIL_PASWORD);
		
	}

}
