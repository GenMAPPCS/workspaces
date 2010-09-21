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

import java.net.URL;
import java.util.ArrayList;


/**
 * Keeps the data related to one download
 * @author Anurag Sharma
 */
public class DownloaderTask {

    private int count = 0;
    private ArrayList<DownloadListener> listenerList;
    private String identifier;
    private URL urlName;
    private static final int MAX_THREAD_COUNT = 16;
    private int startedThreadCount = 0, finishedThreadCount = 0;
//    private boolean threadsStarted[] = new boolean[16];
//    private boolean threadsFinished[] = new boolean[16];
    public int fileSize = 0, downloadedAmount = 0;
    private Status downloadStatus=Status.SUCCESS;

    public DownloaderTask(URL url, String id, int size) {
        listenerList = new ArrayList<DownloadListener>();
        identifier = id;
        urlName = url;
        fileSize = size;
    }

    /**
     *
     * @return the URL being downloaded
     */
    public URL getURL() {
        return urlName;
    }

    /**
     *
     * @return the unique ID for current task
     */
    public String getID() {
        return identifier;
    }

    /**
     *
     * @return the number of threads still actively downloading
     */
    public int getActiveThreadCount() {
        return count;
    }

    /**
     * called when a download thread gets started
     * @param partNumber the index of the downloader thread
     */
    public void notifyStarted(int partNumber) {
        startedThreadCount++;
    }

    /**
     * called when a downloader thread gets finished
     * @param partNumber the index of the thread
     * @param status the exit status of the thread
     */
    public void notifyFinished(int partNumber, Status status) {
        finishedThreadCount++;
        if(status==Status.FAILED)
            downloadStatus=status;
        //check if all Threads have been started and all finished then only notifyListeners
        if (startedThreadCount==MAX_THREAD_COUNT && finishedThreadCount==MAX_THREAD_COUNT) {
            notifyListeners();
        }
    }

    /**
     * adds the listener to be called when the download is finished
     * @param listener the listener to be called
     */
    public void addDownloadListener(DownloadListener listener) {
        listenerList.add(listener);
    }

    private void notifyListeners() {
        DownloadEvent event = new DownloadEvent();
        event.ID = identifier;
        event.url = urlName;
        event.status=downloadStatus;
        if(downloadStatus==Status.FAILED)
            System.out.println("Failed to download File. notifying the listeners now...");
        for (DownloadListener listener : listenerList) {
            listener.onDownloadFinish(event);
        }
    }

    /**
     * adds the supplied amount to the existing progress
     * @param count the amount to add
     */
    public void addToProgress(int count) {
        downloadedAmount += count;
//        System.out.println("+"+downloadedAmount+"="+(int)(100*((float)downloadedAmount)/fileSize));
//        System.out.println("now progress="+getProgress()+"%");
    }

    /**
     *
     * @return the current progress for this download
     */
    public int getProgress() {
        return (int) (100 * ((float) downloadedAmount) / fileSize);
    }

    /**
     * reduces the progress by the supplied amount
     * @param counter the amount by which progress is intended to be reduced.
     */
    public void removeFromProgress(int counter) {
        downloadedAmount -= counter;
    }
}
