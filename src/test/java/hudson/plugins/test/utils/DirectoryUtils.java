package hudson.plugins.test.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

public class DirectoryUtils {

	private static class FileComparator implements Comparator<File>{
		public int compare(File o1, File o2) {
			return o1.getName().compareTo(o2.getName());
		}
	}
	private static final FileComparator FILE_COMPARATOR = new FileComparator();
	
	public static boolean directoryContentsAreEqual(File dir1, File dir2, final Pattern excludePattern, boolean recursive){
		if(!dir1.isDirectory() || !dir2.isDirectory()){
			throw new IllegalArgumentException("dir1 & dir2 should be directories !");
		}
		
		Predicate<File> patternMatcherOnFilenamePredicate = new Predicate<File>() {
			public boolean apply(File f) {
				return excludePattern.matcher(f.getName()).matches();
			}
		};
		
		List<File> dir1Files = new ArrayList<File>( Arrays.asList(dir1.listFiles()) );
		List<File> dir2Files = new ArrayList<File>( Arrays.asList(dir2.listFiles()) );
		
		Collections.sort(dir1Files, FILE_COMPARATOR);
		Collections.sort(dir2Files, FILE_COMPARATOR);
		
		Collection<File> dir1FilesCollec = dir1Files;
		Collection<File> dir2FilesCollec = dir2Files;
		if(excludePattern != null){
			dir1FilesCollec = Collections2.filter(dir1Files, patternMatcherOnFilenamePredicate);
			dir2FilesCollec = Collections2.filter(dir2Files, patternMatcherOnFilenamePredicate);
		}
		
		if(dir1FilesCollec.size() != dir2FilesCollec.size()){
			return false;
		}
		
		
		Iterator<File> dir1FileIter=dir1FilesCollec.iterator();
		Iterator<File> dir2FileIter=dir2FilesCollec.iterator();
		for(int i=0; i<dir1FilesCollec.size(); i++){
			File f1 = dir1FileIter.next();
			File f2 = dir2FileIter.next();
			if(!f1.getName().equals(f2.getName())){
				return false;
			}
			if(f1.isDirectory() != f2.isDirectory()){
				return false;
			}
			if(recursive && f1.isDirectory()){
				if(!directoryContentsAreEqual(f1, f2, excludePattern, recursive)){
					return false;
				}
			}
		}
		
		return true;
	}
	
}
