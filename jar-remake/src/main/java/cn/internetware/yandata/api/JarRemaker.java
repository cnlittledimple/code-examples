package cn.internetware.yandata.api;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

public class JarRemaker {
	
	private static final int BUFFER_SIZE = 1024;
	
	/**
	 * 解压文件
	 * 
	 * @param src 需要解压的压缩文件
	 * @param dst 解压到的目录
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void unpack(String src, String dst) throws FileNotFoundException, IOException {
		File jarFile = new File(src);
		if (dst == null || dst.isEmpty())
			dst = new File(jarFile.getParent(), FilenameUtils.getBaseName(jarFile.getName())).getAbsolutePath();
		dst = dst.endsWith(File.separator) ? dst : dst + File.separator;
		try (JarArchiveInputStream jarIn = new JarArchiveInputStream(new BufferedInputStream(
				new FileInputStream(jarFile), BUFFER_SIZE))) {
			JarArchiveEntry entry = null;
			while ((entry = jarIn.getNextJarEntry()) != null) {
				System.out.println(entry.getName());
				// 忽略路径中的..
				String name = entry.getName().replaceAll("\\\\\\.\\.\\\\", "\\\\")
											.replaceAll("/\\.\\./", "/")
											.replaceAll("^\\.\\./", "")
											.replaceAll("^\\.\\.\\\\", "")
											.replaceAll("/\\.\\.$", "")
											.replaceAll("\\\\\\.\\.$", "");
				if (entry.isDirectory()) {
					File dir = new File(dst, name);
					dir.mkdirs();
				}
				else {
					File file = new File(dst, name);
					File parent = file.getParentFile();
					if (!parent.exists())
						parent.mkdirs();
					try (OutputStream os = new BufferedOutputStream(new FileOutputStream(
							new File(dst, name)), BUFFER_SIZE)) {
						IOUtils.copy(jarIn, os);
					}
				}
			}
		}
	}
	
	/**
	 * 这里获取的路径都是绝对路径
	 * 
	 * @param path
	 * @return
	 */
	private static List<String> getAllFilePaths(String path) {
		List<String> filePaths = new ArrayList<>();
		File file = new File(path);
		if (file.exists()) {
			for (File f : file.listFiles()) {
				String subPath = f.getAbsolutePath();
				if (f.isDirectory()) {
					filePaths.add(subPath.endsWith("/") ? subPath : subPath + "/");
					filePaths.addAll(getAllFilePaths(subPath));
				}
				else 
					filePaths.add(subPath);
			}
		}
		return filePaths;
	}
	
	/**
	 * 将一个文件夹中的内容打包成jar
	 * 
	 * @param src 需要打包的文件夹
	 * @param dst 打包后的目标文件
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void pack(String src, String dst) throws FileNotFoundException, IOException {
		File srcFile = new File(src);
		if (!srcFile.exists()) {
			System.err.println("file " + src + " does not exist");
			return;
		}
		
		if (!srcFile.isDirectory()) {
			System.err.println("file " + src + " is not a directory");
			return;
		}
		
		String pathPrefix = srcFile.getAbsolutePath();
		pathPrefix = pathPrefix.endsWith(File.separator) ? pathPrefix : pathPrefix + File.separator;
		
		if (dst == null || dst.isEmpty())
			dst = srcFile.getName() + ".jar";
		
		File dstFile = new File(dst);
		System.out.println(dstFile.getAbsolutePath());
		
		try (JarArchiveOutputStream jarOut = new JarArchiveOutputStream(new BufferedOutputStream(
				new FileOutputStream(dstFile), BUFFER_SIZE))) {
			List<String> filePaths = getAllFilePaths(src);
			for (String filePath : filePaths) {
				JarArchiveEntry entry = new JarArchiveEntry(filePath.replace(pathPrefix, ""));
				jarOut.putArchiveEntry(entry);
				File file = new File(filePath);
				if (file.isDirectory()) {
					jarOut.closeArchiveEntry();
					continue;
				}
				try (InputStream is = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE)) {
					IOUtils.copy(is, jarOut);
				}
				jarOut.closeArchiveEntry();
			}
		}
	}
	
	private static void printUsage() {
		System.err.println("Usage: java -jar JarRemaker.jar [pack | unpack] [src] [dst]");
		System.err.println();
		System.err.println("pack: compress a directory to a jar file");
		System.err.println("  src: the directory to be compressed");
		System.err.println("  dst: the destination jar file");
		System.err.println();
		System.err.println("unpack: decompress a jar file to a directory");
		System.err.println("  src: the jar file to be decompressed");
		System.err.println("  dst: the destination directory");
	}
	
	public static void main(String[] args) {
		
		if (args.length != 3 && args.length != 2) {
			printUsage();
			System.exit(1);
		}
		
		if ("pack".equals(args[0])) {
			try {
				if (args.length == 3)
					pack(args[1], args[2]);
				else 
					pack(args[1], null);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else if ("unpack".equals(args[0])) {
			try {
				if (args.length == 3)
					unpack(args[1], args[2]);
				else 
					unpack(args[1], null);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			printUsage();
			System.exit(1);
		}
	}
}
