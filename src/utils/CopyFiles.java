package utils;

import java.io.*;

public class CopyFiles{
   public static void copyFile(File srcFile, File destFile) throws IOException
   {
   		InputStream oInStream = new FileInputStream(srcFile);
		OutputStream oOutStream = new FileOutputStream(destFile);

		//Transfer bytes from in to out
		byte[] oBytes = new byte [1024] ;
		int nLength ;
		while ( ( nLength = oInStream . read ( oBytes ) ) > 0){
			oOutStream.write(oBytes,0,nLength);
			oOutStream.flush();
		}
		oInStream.close();
		oOutStream.close();
   }
}