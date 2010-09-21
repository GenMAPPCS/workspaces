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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import org.genmapp.workspaces.tree.SpeciesPanel;

/**
 * Represents the downloader thread which run parallely and download segments of a file
 * @author Anurag Sharma
 */
public class PartsDownloaderThread implements Runnable {

    private int startByte, endByte;
    private int partNumber;
    private HttpURLConnection connection;
    private DownloaderTask task;
    private int readCounter = 0;
    private static final int MAX_RETRY = 5;
    private int retryCounter = 0;

    /**
     * Initiates downloading of specified file segment
     * @param start the start byte of the file segment
     * @param end the end byte of the file segment
     * @param num the index of the current file segment
     * @param dt the DownloaderTask which contains information about the current download
     * @throws IOException
     */
    public PartsDownloaderThread(int start, int end, int num, DownloaderTask dt) throws IOException {
        startByte = start;
        endByte = end;
        partNumber = num;
        task = dt;
    }

    /**
     * starts downloading the specified file segment
     */
    public void start() {
        Thread t = new Thread(this);
        t.start();
    }

    /**
     * Actual downloading code is kept here.
     */
    public synchronized void run() {
        boolean finished = false;
        Status status = Status.SUCCESS; // status = 0 means successfully downloaded , 1- Failed
        task.notifyStarted(partNumber);

        while (!finished && retryCounter < MAX_RETRY) {
            try {

                int len = endByte - startByte + 1;
                byte buffer[] = new byte[1024];

                connection = (HttpURLConnection) task.getURL().openConnection();
                connection.setRequestProperty("range", "bytes=" + startByte + "-" + endByte);
                connection.setUseCaches(true);
                connection.setReadTimeout(15000);
                connection.connect();

                File outputDir = new File(SpeciesPanel.genmappcsdatadir);
                if (!outputDir.exists() || !outputDir.isDirectory()) {
                    outputDir.mkdir();
                }

                int res = connection.getResponseCode();
//                System.out.println("Response=" + connection.getResponseMessage());
                InputStream in = connection.getInputStream();
                OutputStream out = new BufferedOutputStream(new FileOutputStream(SpeciesPanel.genmappcsdatadir + task.getID() + "/" + task.getID() + "part" + partNumber));

                int d = 0;
//                System.out.println("READING part " + partNumber);

                while ((d = in.read(buffer)) != -1) {
                    out.write(buffer, 0, d);
                    readCounter += d;
                    task.addToProgress(d);
                }
                out.flush();
                out.close();

//                System.out.println("DONE " + partNumber);

                finished = true;
                status = Status.SUCCESS;

            } catch (IOException ex) {
                System.out.println(partNumber + ":Unexpected error occurred. Retried for " + retryCounter + " times. Automatic retry after 500ms");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex1) {
                }
                task.removeFromProgress(readCounter);
                retryCounter++;
                readCounter = 0;
                status = Status.FAILED; // for Failure
            }


        }//end of while loop

        //System.out.println("notifying Downloader about this status" + (status == Status.FAILED ? "Failed" : "Success"));
        task.notifyFinished(partNumber, status);
    }
}
