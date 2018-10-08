/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javaapplication3;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Preconditions;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 *
 * @author rmistry@google.com (Ravi Mistry)
 * Alterações (ou adaptações) feitas por Tiago Vitorino
 */
public class JavaApplication3 {

    /**
   * Be sure to specify the name of your application. If the application name is {@code null} or
   * blank, the application will log a warning. Suggested format is "MyCompany-ProductName/1.0".
   */
    private static final String APPLICATION_NAME = "JavaApplication3";
    private static final JsonFactory JSON_FACTORY = (JsonFactory.class.cast(JacksonFactory.getDefaultInstance()));
    private static FileDataStoreFactory dataStoreFactory;
    private static HttpTransport httpTransport;
    JFileChooser jc = new JFileChooser();    
    private static String UPLOAD_FILE_PATH = "C:/teste/teste.txt";         //ARQUIVO APENAS DE TESTE
    private static final String DIR_FOR_DOWNLOADS = "C:/teste";            //PASTA QUE RECEBERÁ OS DOWNLOADS
    private static java.io.File UPLOAD_FILE = new java.io.File(UPLOAD_FILE_PATH);
    
    //private static String client_id_json;
    
    /** Directory to store user credentials. */
    private static final java.io.File DATA_STORE_DIR
            = new java.io.File(System.getProperty("user.home"), ".store/javaapplication3-1");   //SE NÃO FUNCIONAR, ALTERE ESSA PASTA.
    private static Drive drive;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws GeneralSecurityException, IOException {
        
        JOptionPane.showMessageDialog(null, "Escolha o arquivo a ser enviado");
        JavaApplication3 ja = new JavaApplication3();
        int returnVal = ja.jc.showOpenDialog(ja.jc);
        java.io.File f = null;
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            f = ja.jc.getSelectedFile();
        }else{
            System.exit(0);
        }
        UPLOAD_FILE_PATH = f.getAbsolutePath();
        UPLOAD_FILE = f;
        JOptionPane.showMessageDialog(null, "Escolha o arquivo de credencial");
        returnVal = ja.jc.showOpenDialog(ja.jc);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            f = ja.jc.getSelectedFile();
        }else{
            System.exit(0);
        }
        String client_id_json = f.getAbsolutePath();        
        Preconditions.checkArgument(
                !UPLOAD_FILE_PATH.startsWith("Enter ") && !DIR_FOR_DOWNLOADS.startsWith("Enter "),
                "Please enter the upload file path and download directory in %s", JavaApplication3.class);
        try {
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
            // authorization
            Credential credential = ja.authorize(client_id_json);
            // set up the global Drive instance
            drive = new Drive.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(
                    APPLICATION_NAME).build();

            // run commands
            View.header1("Starting Resumable Media Upload");
            File uploadedFile = uploadFile(false);

            View.header1("Updating Uploaded File Name");
            File updatedFile = updateFileWithTestSuffix(uploadedFile.getId());

            View.header1("Starting Resumable Media Download");
            downloadFile(false, updatedFile);

            View.header1("Starting Simple Media Upload");
            uploadedFile = uploadFile(true);

            View.header1("Starting Simple Media Download");
            downloadFile(true, uploadedFile);

            View.header1("Success!");
            return;
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
        System.exit(1);
        // TODO code application logic here
    }

    /**
     * Uploads a file using either resumable or direct media upload.
     */
    private static File uploadFile(boolean useDirectUpload) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setTitle(UPLOAD_FILE.getName());

        FileContent mediaContent = new FileContent("image/jpeg", UPLOAD_FILE);

        Drive.Files.Insert insert = drive.files().insert(fileMetadata, mediaContent);
        MediaHttpUploader uploader = insert.getMediaHttpUploader();
        uploader.setDirectUploadEnabled(useDirectUpload);
        uploader.setProgressListener(new FileUploadProgressListener());
        return insert.execute();
    }

    /**
     * Updates the name of the uploaded file to have a "drivetest-" prefix.
     */
    private static File updateFileWithTestSuffix(String id) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setTitle("drivetest-" + UPLOAD_FILE.getName());

        Drive.Files.Update update = drive.files().update(id, fileMetadata);
        return update.execute();
    }
    
    /** Downloads a file using either resumable or direct media download. */
  private static void downloadFile(boolean useDirectDownload, File uploadedFile)
      throws IOException {
    // create parent directory (if necessary)
    java.io.File parentDir = new java.io.File(DIR_FOR_DOWNLOADS);
    if (!parentDir.exists() && !parentDir.mkdirs()) {
      throw new IOException("Unable to create parent directory");
    }
    OutputStream out = new FileOutputStream(new java.io.File(parentDir, uploadedFile.getTitle()));

    MediaHttpDownloader downloader =
        new MediaHttpDownloader(httpTransport, drive.getRequestFactory().getInitializer());
    downloader.setDirectDownloadEnabled(useDirectDownload);
    downloader.setProgressListener(new FileDownloadProgressListener());
    downloader.download(new GenericUrl(uploadedFile.getDownloadUrl()), out);
  }

    private static void printFile(Drive service, String fileId) {

        try {

            File file = service.files().get(fileId).execute();

            System.out.println("Title: " + file.getTitle());
            System.out.println("Description: " + file.getDescription());
            System.out.println("MIME type: " + file.getMimeType());
        } catch (IOException e) {
            System.out.println("An error occured: " + e);
        }
    }

    private Credential authorize(String client_id_json_caminho) throws Exception {
        // load client secrets
        InputStream is = new FileInputStream(client_id_json_caminho);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(is,"UTF-8"));//JavaApplication3.class.getResourceAsStream(client_id_json)));
        if (clientSecrets.getDetails().getClientId().startsWith("Enter")
                || clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
            System.out.println(
                    "Enter Client ID and Secret from https://code.google.com/apis/console/?api=drive "
                    + "into drive-cmdline-sample/src/main/resources/client_secrets.json");
            System.exit(1);
        }
        // set up authorization code flow
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets,
                Collections.singleton(DriveScopes.DRIVE_FILE)).setDataStoreFactory(dataStoreFactory)
                .build();
        // authorize
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    /**
     * Download a file's content.
     *
     * @param service Drive API service instance.
     * @param file Drive File instance.
     * @return InputStream containing the file's content if successful,
     * {@code null} otherwise.
     */
    private static InputStream downloadFile(Drive service, File file) {
        if (file.getDownloadUrl() != null && file.getDownloadUrl().length() > 0) {
            // try {
            // HttpResponse resp = service.files().
            // service..getRequestFactory().buildGetRequest(new GenericUrl(file.getDownloadUrl()))
            //   .execute();
            //  return resp.getContent();
            //  } catch (IOException e) {
            // An error occurred.
            //e.printStackTrace();
            return null;
            //}
            //  } else {
            // The file doesn't have any content stored on Drive.
            //   return null;
        }
        return null;
    }
}
