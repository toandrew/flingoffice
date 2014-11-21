package com.infthink.myflingoffice;

import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.NonReadableChannelException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    public static final String REQUEST_JSON_NAME = "request";

    public static final String REQUEST_JSON_KEY_CMD = "cmd";

    public static String RESPONSE_JSON_RESULT_KEY = "result";

    public static final String KEY_HTTP_BASE_URL = "http://castapp.infthink.com:8765/";

    public static final int HANDLE_COMMUNICATION_RESPONSE_BODY_ENTITY = 0;

    public static final int HANDLE_COMMUNICATION_RESPONSE_ERROR = 1;

    public static final int HANDLE_CONNECTION_FAIL = -1;

    public static final String HTTP_ENCODE = "utf-8";

    public static final int RESULT_SUCCESS = 0;

    private static MessageDigest md5 = null;

    static {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static Object String2MD5(String name) {
        StringBuilder hexString = new StringBuilder("");
        md5.reset();

        md5.update(name.getBytes());

        BigInteger bi = new BigInteger(1, md5.digest());

        hexString.append(bi.toString());
        return hexString;
    }

    public static String File2MD5(File file) {
        FileInputStream input = null;
        StringBuilder hexString = new StringBuilder("");
        md5.reset();
        try {
            input = new FileInputStream(file);

            MappedByteBuffer byteBuffer = input.getChannel().map(
                    FileChannel.MapMode.READ_ONLY, 0, file.length());

            md5.update(byteBuffer);

            BigInteger bi = new BigInteger(1, md5.digest());

            hexString.append(bi.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NonReadableChannelException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } finally {
            try {
                if (input != null)
                    input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return hexString.toString();
    }

    public static String BigFile2MD5(File file) {
        FileInputStream fis = null;
        StringBuilder hexString = new StringBuilder("");
        md5.reset();
        try {
            fis = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) != -1) {
                md5.update(buffer, 0, length);
            }

            BigInteger bi = new BigInteger(1, md5.digest());

            hexString.append(bi.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null)
                    fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return hexString.toString();
    }

    public static String File2Base64(File file) {
        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (file != null) {
                baos = new ByteArrayOutputStream();

                FileInputStream input = new FileInputStream(file);

                byte[] buffer = new byte[4096];
                int n = 0;
                while (-1 != (n = input.read(buffer))) {
                    baos.write(buffer, 0, n);
                }

                byte[] fileBytes = baos.toByteArray();
                result = Base64.encodeToString(fileBytes, Base64.DEFAULT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NonReadableChannelException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static File Base64File(String filename, String base64code) {
        File f = new File(filename);
        FileOutputStream output;
        try {
            output = new FileOutputStream(f);
            output.write(Base64.decode(base64code, Base64.DEFAULT));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return f;
    }
}
