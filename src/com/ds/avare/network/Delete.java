/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.network;

import java.io.File;
import java.util.LinkedList;
import java.util.Observable;

import com.ds.avare.storage.DataSource;
import android.os.Handler;
import android.os.Message;

/**
 * 
 * @author zkhan
 *
 */
public class Delete extends Observable {
    
    private DeleteTask   mDt;
    private boolean     mStop;
    private Handler      mHandler;
    private Thread       mThread;
   
    public static final int FAILED = -2;
    public static final int SUCCESS = -1;
    
       
    /**
     * 
     * @param act
     */
    public Delete(Handler handler) {
        mStop = false;
        mDt = null;
        mHandler = handler;
    }
    
    /**
     * 
     */
    public void cancel() {
        mStop = true;
    }

    /**
     * 
     * @param url
     * @param path
     * @param dataSource 
     * @param filename
     */
    public void start(String path, String name, DataSource dataSource) {
        mDt = new DeleteTask();
        mDt.path = path;
        mDt.chart = name;
        mDt.data = dataSource;
        mThread = new Thread(mDt);
        mThread.start();
    }

    /**
     * 
     * @author zkhan
     *
     */
    private class DeleteTask implements Runnable {

        public String path;
        public String chart;
        public DataSource data;
        
        /**
         * 
         */
        @Override
        public void run() {
            
            Thread.currentThread().setName("Delete");

            if(data == null || path == null || chart == null) {
                Message m = mHandler.obtainMessage(Download.FAILED, Delete.this);
                mHandler.sendMessage(m);
            }
            LinkedList<String> list = data.findFilesToDelete(chart);
            
            int fileLength = list.size();
            int total = 0;
            int newp;
            int lastp = FAILED;

            for(String name : list) {
                
                if(mStop) {
                    Message m = mHandler.obtainMessage(Download.FAILED, Delete.this);
                    mHandler.sendMessage(m);
                    return;
                }
                newp = (int) (total * 50 / fileLength);
                
                String toDelete = path + "/" + name;
                
                try {
                    (new File(toDelete)).delete();
                }
                catch(Exception e) {
                    
                }
                
                if(lastp != newp) {
                    lastp = newp;
                    Message m = mHandler.obtainMessage(newp, Delete.this);
                    mHandler.sendMessage(m);
                }
            }
            
            Message m = mHandler.obtainMessage(Download.SUCCESS, Delete.this);
            mHandler.sendMessage(m);
        }      
    }
}
