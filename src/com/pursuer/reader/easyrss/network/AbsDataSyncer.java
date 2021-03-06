/*******************************************************************************
 * Copyright (c) 2012 Pursuer (http://pursuer.me).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Pursuer - initial API and implementation
 ******************************************************************************/

package com.pursuer.reader.easyrss.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.apache.http.protocol.HTTP;

import com.pursuer.reader.easyrss.account.ReaderAccountMgr;
import com.pursuer.reader.easyrss.data.DataMgr;
import com.pursuer.reader.easyrss.data.readersetting.SettingHttpsConnection;
import com.pursuer.reader.easyrss.network.NetworkClient.NetworkException;
import com.pursuer.reader.easyrss.network.url.AbsURL;

public abstract class AbsDataSyncer {
    public class DataSyncerException extends Exception {
        private static final long serialVersionUID = 1L;

        public DataSyncerException(final Exception exception) {
            super(exception.getMessage(), exception.getCause());
        }

        public DataSyncerException(final String message) {
            super(message);
        }

        public DataSyncerException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public DataSyncerException(final Throwable cause) {
            super(cause);
        }
    }

    final protected static int CONTENT_IO_BUFFER_SIZE = 16384;
    final protected static int UNREAD_COUNT_LIMIT = 500;
    final protected static int GLOBAL_ITEMS_LIMIT = 300;
    final protected static int GLOBAL_ITEM_IDS_LIMIT = 600;
    final protected static int ITEM_LIST_QUERY_LIMIT = 20;

    public static final long TOKEN_EXPIRE_TIME = 2 * 60 * 1000;

    final protected DataMgr dataMgr;
    final protected boolean isHttpsConnection;
    final protected int networkConfig;
    private DataSyncerListener listener;
    private Boolean isPending;
    private Boolean isRunning;

    public AbsDataSyncer(final DataMgr dataMgr, final int networkConfig) {
        this.isPending = false;
        this.isRunning = false;
        this.dataMgr = dataMgr;
        this.networkConfig = networkConfig;
        this.isHttpsConnection = new SettingHttpsConnection(dataMgr).getData();
    }

    protected abstract void finishSyncing();

    public DataSyncerListener getListener() {
        return listener;
    }

    public int getNetworkConfig() {
        return networkConfig;
    }

    protected byte[] httpGetQueryByte(final AbsURL url) throws DataSyncerException {
        final NetworkClient client = NetworkClient.getInstance();
        if (url.isAuthNeeded()) {
            final String auth = ReaderAccountMgr.getInstance().blockingGetAuth();
            client.setAuth(auth);
        }
        try {
            String rUrl = url.getURL();
            final String param = url.getParamsString();
            if (param.length() > 0) {
                rUrl += "?" + param;
            }
            return client.doGetByte(rUrl);
        } catch (final Exception e) {
            throw new DataSyncerException(e);
        }
    }

    protected Reader httpGetQueryReader(final AbsURL url) throws DataSyncerException {
        try {
            return new InputStreamReader(httpGetQueryStream(url), HTTP.UTF_8);
        } catch (final UnsupportedEncodingException e) {
            throw new DataSyncerException(e);
        }
    }

    protected InputStream httpGetQueryStream(final AbsURL url) throws DataSyncerException {
        final NetworkClient client = NetworkClient.getInstance();
        if (url.isAuthNeeded()) {
            final String auth = ReaderAccountMgr.getInstance().blockingGetAuth();
            client.setAuth(auth);
        }
        try {
            String rUrl = url.getURL();
            final String param = url.getParamsString();
            if (param.length() > 0) {
                rUrl += "?" + param;
            }
            return client.doGetStream(rUrl);
        } catch (final Exception exception) {
            throw new DataSyncerException(exception);
        }
    }

    protected byte[] httpPostQueryByte(final AbsURL url) throws DataSyncerException {
        final NetworkClient client = NetworkClient.getInstance();
        if (url.isAuthNeeded()) {
            final String auth = ReaderAccountMgr.getInstance().blockingGetAuth();
            client.setAuth(auth);
        }
        try {
            String rUrl = url.getURL();
            final String param = url.getParamsString();
            if (param.length() > 0) {
                rUrl += "?" + param;
            }
            return client.doPostByte(rUrl, param);
        } catch (final Exception exception) {
            throw new DataSyncerException(exception);
        }
    }

    protected Reader httpPostQueryReader(final AbsURL url) throws DataSyncerException {
        try {
            return new InputStreamReader(httpPostQueryStream(url), HTTP.UTF_8);
        } catch (final UnsupportedEncodingException exception) {
            throw new DataSyncerException(exception);
        }
    }

    protected InputStream httpPostQueryStream(final AbsURL url) throws DataSyncerException {
        final NetworkClient client = NetworkClient.getInstance();
        if (url.isAuthNeeded()) {
            final String auth = ReaderAccountMgr.getInstance().blockingGetAuth();
            client.setAuth(auth);
        }
        try {
            return NetworkClient.getInstance().doPostStream(url.getURL(), url.getParamsString());
        } catch (final IOException exception) {
            throw new DataSyncerException(exception);
        } catch (final NetworkException exception) {
            throw new DataSyncerException(exception);
        }
    }

    public boolean isPending() {
        synchronized (this.isPending) {
            return isPending;
        }
    }

    public boolean isRunning() {
        synchronized (this.isRunning) {
            return isRunning;
        }
    }

    protected void notifyProgressChanged(final String text, final int progress, final int maxProgress) {
        if (listener != null) {
            listener.onProgressChanged(text, progress, maxProgress);
        }
    }

    protected String parseContent(final Reader in) throws DataSyncerException {
        final StringBuilder builder = new StringBuilder();
        try {
            final char buff[] = new char[8192];
            int len;
            final BufferedReader reader = new BufferedReader(in, 8192);
            while ((len = reader.read(buff, 0, buff.length)) != -1) {
                builder.append(buff, 0, len);
            }
            return builder.toString();
        } catch (IOException e) {
            throw new DataSyncerException(e);
        } finally {
            try {
                in.close();
            } catch (final IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    public boolean setEnterPending() {
        synchronized (this.isPending) {
            if (isPending) {
                return false;
            } else {
                isPending = true;
                return true;
            }
        }
    }

    private boolean setEnterRunning() {
        synchronized (this.isRunning) {
            if (isRunning) {
                return false;
            } else {
                isRunning = true;
                return true;
            }
        }
    }

    public void setListener(final DataSyncerListener listener) {
        this.listener = listener;
    }

    public void setPending(final boolean isPending) {
        synchronized (this.isPending) {
            this.isPending = isPending;
        }
    }

    private void setRunning(final boolean isRunning) {
        synchronized (this.isRunning) {
            this.isRunning = isRunning;
        }
    }

    protected abstract void startSyncing() throws DataSyncerException;

    public void sync() throws DataSyncerException {
        if (!setEnterRunning()) {
            return;
        }
        DataSyncerException except = null;
        try {
            startSyncing();
        } catch (final DataSyncerException exception) {
            except = exception;
        }
        finishSyncing();
        setRunning(false);
        if (except != null) {
            throw except;
        }
    }
}
