/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Peter Smith
 *******************************************************************************/
package org.boris.winrun4j.winapi;

import java.nio.ByteBuffer;

import org.boris.winrun4j.Native;
import org.boris.winrun4j.NativeHelper;

public class FileManagement
{
    public static final int MOVEFILE_REPLACE_EXISTING = 0x00000001;
    public static final int MOVEFILE_COPY_ALLOWED = 0x00000002;
    public static final int MOVEFILE_DELAY_UNTIL_REBOOT = 0x00000004;
    public static final int MOVEFILE_WRITE_THROUGH = 0x00000008;
    public static final int MOVEFILE_CREATE_HARDLINK = 0x00000010;
    public static final int MOVEFILE_FAIL_IF_NOT_TRACKABLE = 0x00000020;

    public static final int FILE_ACTION_ADDED = 1;
    public static final int FILE_ACTION_REMOVED = 2;
    public static final int FILE_ACTION_MODIFIED = 3;
    public static final int FILE_ACTION_RENAMED_OLD_NAME = 4;
    public static final int FILE_ACTION_RENAMED_NEW_NAME = 5;

    public static final int FILE_NOTIFY_CHANGE_FILE_NAME = 0x1;
    public static final int FILE_NOTIFY_CHANGE_DIR_NAME = 0x2;
    public static final int FILE_NOTIFY_CHANGE_ATTRIBUTES = 0x4;
    public static final int FILE_NOTIFY_CHANGE_SIZE = 0x8;
    public static final int FILE_NOTIFY_CHANGE_LAST_WRITE = 0x10;
    public static final int FILE_NOTIFY_CHANGE_LAST_ACCESS = 0x20;
    public static final int FILE_NOTIFY_CHANGE_CREATION = 0x40;
    public static final int FILE_NOTIFY_CHANGE_SECURITY = 0x100;

    public static boolean moveFileEx(String existingName, String newName, int flags) {
        if (existingName == null || newName == null)
            throw new NullPointerException();
        long e = NativeHelper.toNativeString(existingName, true);
        long n = NativeHelper.toNativeString(newName, true);
        boolean res = NativeHelper.call(Kernel32.library, "MoveFileExW", e, n, flags) == 1;
        NativeHelper.free(e, n);
        return res;
    }

    public static String getCurrentDirectory() {
        long lpBuffer = Native.malloc(Shell32.MAX_PATHW);
        int count = (int) NativeHelper.call(Kernel32.library, "GetCurrentDirectoryW", Shell32.MAX_PATHW, lpBuffer);
        String res = null;
        if (count > 0)
            res = NativeHelper.getString(lpBuffer, Shell32.MAX_PATHW, true);
        NativeHelper.free(lpBuffer);
        return res;
    }

    public static long FindFirstFile(String fileName, WIN32_FIND_DATA findFileData) {
        if (findFileData == null)
            return 0;
        long lpFileName = NativeHelper.toNativeString(fileName, true);
        long lpFindFileData = Native.malloc(WIN32_FIND_DATA.SIZE);
        long handle = NativeHelper.call(Kernel32.library, "FindFirstFile", lpFileName, lpFindFileData);
        if (handle != 0) {
            ByteBuffer bb = NativeHelper.getBuffer(lpFindFileData, WIN32_FIND_DATA.SIZE);
            findFileData.dwFileAttributes = bb.getInt();
            findFileData.ftCreationTime = decodeFileTime(bb);
            findFileData.ftLastAccessTime = decodeFileTime(bb);
            findFileData.ftLastWriteTime = decodeFileTime(bb);
            findFileData.nFileSizeHigh = bb.getInt();
            findFileData.nFileSizeLow = bb.getInt();
            findFileData.dwReserved0 = bb.getInt();
            findFileData.dwReserved1 = bb.getInt();
            byte[] cbfn = new byte[Shell32.MAX_PATHW];
            bb.get(cbfn);
            findFileData.cFileName = NativeHelper.toString(cbfn);
            byte[] cbaf = new byte[28];
            bb.get(cbaf);
            findFileData.cAlternateFileName = NativeHelper.toString(cbaf);
        }
        NativeHelper.free(lpFileName, lpFindFileData);
        return handle;
    }

    public static boolean setCurrentDirectory(String pathName) {
        long lpPathName = NativeHelper.toNativeString(pathName, true);
        boolean res = NativeHelper.call(Kernel32.library, "SetCurrentDirectoryW", lpPathName) != 0;
        NativeHelper.free(lpPathName);
        return res;
    }

    private static FILETIME decodeFileTime(ByteBuffer bb) {
        FILETIME res = new FILETIME();
        res.dwLowDateTime = bb.getInt();
        res.dwHighDateTime = bb.getInt();
        return res;
    }

    public static class WIN32_FIND_DATA
    {
        public static final int SIZE = 592;
        public int dwFileAttributes;
        public FILETIME ftCreationTime;
        public FILETIME ftLastAccessTime;
        public FILETIME ftLastWriteTime;
        public int nFileSizeHigh;
        public int nFileSizeLow;
        public int dwReserved0;
        public int dwReserved1;
        public String cFileName;
        public String cAlternateFileName;
    }

    public static class FILETIME
    {
        public int dwLowDateTime;
        public int dwHighDateTime;
    }
}
