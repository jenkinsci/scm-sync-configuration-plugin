package hudson.plugins.test.utils;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class DirectoryUtils {

    private static final Logger LOGGER = Logger.getLogger(DirectoryUtils.class.getName());
    
	private static class FileComparator implements Comparator<File>{
		public int compare(File o1, File o2) {
			return o1.getName().compareTo(o2.getName());
		}
	}
	private static final FileComparator FILE_COMPARATOR = new FileComparator();
	
    public static List<String> diffDirectories(File dir1, File dir2, final List<Pattern> excludePatterns, boolean recursive){
        List<String> diffs = new ArrayList<String>();

		if(!dir1.isDirectory() || !dir2.isDirectory()){
			throw new IllegalArgumentException("dir1 & dir2 should be directories !");
		}
		
		Predicate<File> patternMatcherOnFilenamePredicate = new Predicate<File>() {
			public boolean apply(File f) {
				for(Pattern pattern : excludePatterns) {
					if (pattern.matcher(f.getName()).matches()) {
						return false;
					}
				}
				return true;
			}
		};
		
		List<File> dir1Files = new ArrayList<File>( Arrays.asList(dir1.listFiles()) );
		List<File> dir2Files = new ArrayList<File>( Arrays.asList(dir2.listFiles()) );
		
		Collections.sort(dir1Files, FILE_COMPARATOR);
		Collections.sort(dir2Files, FILE_COMPARATOR);
		
		Collection<File> dir1FilesCollec = dir1Files;
		Collection<File> dir2FilesCollec = dir2Files;
		if(excludePatterns != null){
			dir1FilesCollec = Collections2.filter(dir1Files, patternMatcherOnFilenamePredicate);
			dir2FilesCollec = Collections2.filter(dir2Files, patternMatcherOnFilenamePredicate);
		}
		
		if(dir1FilesCollec.size() != dir2FilesCollec.size()){
            diffs.add(String.format("Number of files in %s (%s) and %s (%s) differ !", dir1, dir1FilesCollec.size(), dir2, dir2FilesCollec.size()));
            return diffs;
		}
		
		
		Iterator<File> dir1FileIter=dir1FilesCollec.iterator();
		Iterator<File> dir2FileIter=dir2FilesCollec.iterator();
		for(int i=0; i<dir1FilesCollec.size(); i++){
			File f1 = dir1FileIter.next();
			File f2 = dir2FileIter.next();
			if(!f1.getName().equals(f2.getName())){
                diffs.add(String.format("File listing in %s (%s) and %s (%s) differ !", dir1, dir1FilesCollec, dir2, dir2FilesCollec));
                return diffs;
			}
			if(f1.isDirectory() != f2.isDirectory()){
                diffs.add(String.format("Path type is not the same (dir / dir or file / file) on %s and %s !", f1, f2));
			}
			if(f1.isFile()){
				try {
					if(!FileUtils.contentEquals(f1, f2)){
                        diffs.add(String.format("File content differ between %s and %s !", f1, f2));
					}
				}catch(IOException e){
					LOGGER.throwing(FileUtils.class.getName(), "contentEquals", e);
					LOGGER.severe("Error occured when comparing <"+f1.getAbsolutePath()+"> content with <"+f2.getAbsolutePath()+"> content");
                    diffs.add("Error occured when comparing <"+f1.getAbsolutePath()+"> content with <"+f2.getAbsolutePath()+"> content");
				}
			}
			if(recursive && f1.isDirectory()){
                diffs.addAll(diffDirectories(f1, f2, excludePatterns, recursive));
			}
		}
		
		return diffs;
	}
	
}
