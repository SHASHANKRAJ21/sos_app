package com.example.sos_app;
import android.os.AsyncTask;

public class SendMailTask extends AsyncTask<Void, Void, Void> {
    private String recipientEmail;
    private String subject;
    private String messageBody;
    private String filePath; // The path to the audio file
    private MailSender mailSender;

    public SendMailTask(String recipientEmail, String subject, String messageBody, String filePath, MailSender mailSender) {
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.messageBody = messageBody;
        this.filePath = filePath;
        this.mailSender = mailSender;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            // Send the email with the attachment
            mailSender.sendMailWithAttachment(subject, messageBody, recipientEmail, filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
