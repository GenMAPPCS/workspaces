/*******************************************************************************
 * Copyright 2010 Alexander Pico
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.genmapp.workspaces.utils;

//package downloader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.genmapp.workspaces.tree.SpeciesPanel;

/**
 * Downloads any URL supplied from internet
 * @author Anurag Sharma
 */
public class Downloader implements DownloadListener {

    private DownloaderTask task;
    private String identifier;
    private int sizeOfFile = 0;
//    private int downloadedAmount = 0;
    private static final int JET_COUNT = 16;
    private JProgressBar pbar;
    private File outputFile;
    private Status downloadStatus = Status.NOT_STARTED;
    private Status exitStatus = Status.SUCCESS;

    public Downloader() {
    }

    /**
     * downloads the URL supplied
     * @param urlName the URL to download
     * @throws MalformedURLException
     * @throws IOException
     */
    public void download(String urlName) throws MalformedURLException, IOException {
//        JOptionPane.showMessageDialog(null, "This program is trying to download a file from internet.","Information",
//                JOptionPane.INFORMATION_MESSAGE);
        System.out.println("connecting " + urlName);

        URL url = new URL(urlName);

        String ID = generateID();

        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        int len = c.getContentLength();
        sizeOfFile = len;
        task = new DownloaderTask(url, identifier, sizeOfFile);
        task.addDownloadListener(this);

        int chunkCount = JET_COUNT;
        int chunkSize = len / chunkCount;
        int s = 0, e = chunkSize;
        System.out.println("Total size=" + len);

        downloadStatus = Status.DOWNLOADING;

        File downloadParentDirectory = new File(SpeciesPanel.genmappcsdatadir);
        if (!downloadParentDirectory.exists()) {
            downloadParentDirectory.mkdir();
        }

        File tempDirectory = new File(SpeciesPanel.genmappcsdatadir + ID);
        if (!tempDirectory.exists()) {
            tempDirectory.mkdir();
        }

        for (int i = 0; i < chunkCount; i++) {
            if (i == chunkCount - 1) {
                e = len;
            }
            PartsDownloaderThread t = new PartsDownloaderThread(s, e, i, task);
            t.start();
//            System.out.println("start:" + s + " end:" + e);
            s = e + 1;
            e += chunkSize;
        }

//        showProgressBar();

//        int p = 0;
//        while ((p = task.getProgress()) <= 100) {
////            System.out.println(p + "% complete. " + task.getActiveThreadCount() + " threads active.");
////            System.out.println((task.downloadedAmount / 1024) + "KB of " + (task.fileSize / 1024) + "KB");
//            if (pbar != null) {
//                pbar.setValue(p);
//                pbar.repaint();
//            }
//            if (p == 100) {
//                break;
//            }
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException ex) {
//                Logger.getLogger(Downloader.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//
//        System.out.println("finally " + task.getActiveThreadCount() + " threads still active");

    }

    /**
     * adds a listener to be notified when the download is finished
     * @param listener the listener to be notified
     */
    public void addDownloadListener(DownloadListener listener) {
        task.addDownloadListener(listener);
    }

    /**
     * Called when download is finished. Performs joining operation of the various file segments which are downloaded
     * @param evt
     */
    public synchronized void onDownloadFinish(DownloadEvent evt) {
        if (evt.status == Status.FAILED) {
            //handle it here
            if (pbar != null) {
                pbar.setValue(0);
                pbar.setString("Failed to download...");
            }
            System.out.println("Failed! Handling code here... ");
            exitStatus = evt.status;
            downloadStatus = Status.FINISHED;
            return;
        }

        String outputFileName = task.getURL().getFile();
        int last = 0;
        for (int i = outputFileName.length() - 1; i >= 0 && outputFileName.charAt(i) != '/'; i--) {
            last = i;
        }
        outputFileName = outputFileName.substring(last, outputFileName.length());

        //check if the fileName exists in current directory, if so rename it to filename_1 and so on

        File testFile = new File(SpeciesPanel.genmappcsdatadir + outputFileName);
        if (testFile.exists()) {
            String newName = null;
            for (int i = 1; testFile.exists(); i++) {
                newName = outputFileName + "_" + i;
                testFile = new File(SpeciesPanel.genmappcsdatadir + newName);
            }
            outputFileName = newName;
        }

//        throw new UnsupportedOperationException("Not supported yet.");
//        System.out.println("TODO: initiate the Combining operation now");

        if (pbar != null) {
            pbar.setMaximum(task.fileSize);
            pbar.setString("rebuilding File...");
        }
        try {
            outputFile = new File(SpeciesPanel.genmappcsdatadir+ outputFileName);
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile, true));
            byte buffer[] = new byte[1024];

            for (int i = 0; i < JET_COUNT; i++) {
                File file = new File(SpeciesPanel.genmappcsdatadir + identifier + "/" + identifier + "part" + i);
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
                int d = 0;
                while ((d = in.read(buffer)) != -1) {
                    out.write(buffer, 0, d);
                    if (pbar != null) {
                        pbar.setValue(pbar.getValue() + d);
                    }
                }
                in.close();
                file.delete();
            }

            File tempDirectory = new File(SpeciesPanel.genmappcsdatadir+ identifier);
            tempDirectory.delete();

            out.close();
            if (pbar != null) {
                pbar.setValue(pbar.getMaximum());
                pbar.setString("Finished");
            }
            System.out.println("Successfully finished downloading the file");
            downloadStatus = Status.FINISHED;

        } catch (IOException ex) {
            ex.printStackTrace();
        }


        System.out.println(task.downloadedAmount + "/" + task.fileSize);
        System.out.println("progress=" + task.getProgress() + "%");

    }

    /**
     * Waits for the download to finish. Checks the status every 300ms.
     */
    public void waitFor() {
        while (downloadStatus != Status.FINISHED) {
            try {
                Thread.sleep(300);
            } catch (Exception ex) {
            }
        }
    }

    /**
     *
     * @return the downloaded file.
     */
    public File getOutputFile() {
        if (exitStatus == Status.FAILED) {
            System.out.println("exit status found Failed. returning null");
            return null;
        }

        return outputFile;
    }

    /**
     *
     * @return the progress of download
     */
    public int getProgress() {
        return task.getProgress();
    }

    private String generateID() {
        String ID = String.valueOf((int) (Math.random() * 10000));
        identifier = ID;
        return ID;
    }

//    public void run() {
//            try {
//                while ((getStatus() != STATUS.COMPLETED) && (getStatus() != STATUS.STOPPED)) {
//
//                    /**
//                     * setting http connection for required bytes
//                     */
//                    connection = (HttpURLConnection) url.openConnection();
//                    connection.setRequestProperty("Range",
//                            "bytes=" + (offset + downloaded) + "-" + (offset + chunksize - 1));
//                    notify(STATUS.FETCHING);
//                    connection.setUseCaches(true);
//                    connection.setReadTimeout(15000);
//                    connection.connect();
//                    response = connection.getResponseCode();
//
//                    switch (response) {
//                        case HttpURLConnection.HTTP_UNAVAILABLE:
//                            notify(STATUS.ERROR);
//                    }
//
//                    if (response / 100 != 2) {
//                        System.out.println("error");
//                        notify(STATUS.ERROR);
//
//                        throw new IOException(connection.getResponseMessage() + "-" + connection.getResponseCode());
//                    }
//
//                    notify(STATUS.CONNECTED);
//                    time = new Date().getTime();
//                    writeData(file);
//
//                    /**
//                     * notify downloading started
//                     */
//                    notify(STATUS.DOWNLOADING);
//
//                    /**
//                     * stream initation
//                     */
//                    stream = connection.getInputStream();
//
//                    while (getStatus() != STATUS.STOPPED) {
//                        Buffer data = new Buffer(bufferSize);
//
//
//                        // byte[] data = new byte[bufferSize];
//                        // int read = 0;
//                        // int seek = 0;
//                        int read = stream.read(data.data);
//
//                        // while (seek < bufferSize) {
//                        data.read = read;
//
//                        // read = stream.read(data, seek, bufferSize - seek);
//                        if (getStatus() == STATUS.PAUSED) {
//                            final DownloadTask t = task;
//                            try {
//                                synchronized (t) {
//                                    //stream.mark(downloaded);
//                                    //stream.close();
//                                    t.wait();
//                                    break;
//                                    //stream = connection.getInputStream();
//                                }
//                            } catch (InterruptedException ex) {
//                                Logger.getLogger(DownloadTask.class.getName()).log(Level.SEVERE, null, ex);
//                            }
//                        }
//
//                        if (read != -1) {
//
//                            // seek += read;
//                            downloaded += read;
//
//                            try {
//                                notify(STATUS.DOWNLOADING);
//                                // buffer.put(data);
//                                buffer.put(data);
//                            } catch (InterruptedException ex) {
//                                Logger.getLogger(DownloadTask.class.getName()).log(Level.SEVERE, null, ex);
//                            }
//                        } else if (read == -1) {
//
//                            // if (downloaded != chunksize) {
//                            notify(STATUS.COMPLETED);
//
//                            break;
//                        } else {
//                            notify(STATUS.STOPPED);
//
//                            break;
//                        }
//
//                        // }   //data.read = read;
//                    }
//                }
//
//                notify(status);
//                stream.close();
//                connection.disconnect();
//
//                // buffer.clear();
//            } catch (IOException ex) {
//                Logger.getLogger(DownloadTask.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//    public static void main(String[] args) throws MalformedURLException, IOException {
//        Downloader m = new Downloader();
//        m.download("http://localhost/test.zip");
//        m.download("http://localhost/normalized_mESC_differentiation.zip");
//        m.download("http://localhost/unprocessedCELFiles__GSE13297_RAW.tar");
//        m.download("http://altanalyze.org/archiveDBs/LibraryFiles/HG-U133A.zip");
//        m.download("http://altanalyze.org/archiveDBs/LibraryFiles/HuGene-1_0-st-v1.r4.bgp.gz"); //success
//        m.download("http://altanalyze.org/archiveDBs/LibraryFiles/MoGene-1_0-st-v1.r4.clf.gz"); //success
//        m.download("http://altanalyze.org/archiveDBs/LibraryFiles/RAE230B.zip");
//
//    }
    
    public int getPercentComplete(){
    	return task.getProgress();
    }

    private void showProgressBar() {
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(f.EXIT_ON_CLOSE);
        f.setSize(400, 200);
        JPanel p = new JPanel();
        pbar = new JProgressBar(0, 100);
        pbar.setStringPainted(true);
        p.add(pbar);
        f.add(p);
        f.setVisible(true);
    }

    /**
     * shows the progress in the supplied progress bar
     * @param jProgressBar1 the progress bar where the progress is intended to be shown
     */
    public void showProgressIn(JProgressBar jProgressBar1) {
        this.pbar = jProgressBar1;
        new Thread() {

            @Override
            public void run() {
                while (downloadStatus != Status.FINISHED) {
                    try {
                        pbar.setValue(task.getProgress());
                        pbar.repaint();
                        Thread.sleep(100);
                    } catch (Exception ex) {
                        Logger.getLogger(Downloader.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }.start();
    }
}
