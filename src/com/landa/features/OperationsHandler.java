package com.landa.features;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.fileexplorermanager.R;
import com.landa.adapter.MainFileListAdapter;
import com.landa.datatypes.ClipboardFile;
import com.landa.datatypes.SelectedFile;
import com.landa.dialog.ClipboardDialogFragment;
import com.landa.dialog.CreateNewDialogFragment;
import com.landa.dialog.FilePropertiesDialogFragment;
import com.landa.dialog.OperationsDialogFragment;
import com.landa.fileexplorermanager.MainActivity;
import com.landa.general.DirectoryZip;
import com.landa.general.FileZip;
import com.landa.general.General;


public class OperationsHandler {
	
	
    static private OperationsHandler instance;

    static public void init(Context ctx, FragmentActivity ac) {
        //if (null==instance) {
            instance = new OperationsHandler(ctx, ac);
        //}
    }
    static public OperationsHandler getInstance() {
        return instance;
    }
	
	private Context ctx;
	private FragmentActivity ac;
	
	public OperationsHandler(Context ctx, FragmentActivity ac)
	{
		this.ctx = ctx;
		this.ac = ac;
	}
	
	
	public void openOperationsDialog(File f) 
	{
		OperationsDialogFragment opDialog = new OperationsDialogFragment();
		
    	Bundle bdl = new Bundle(1);
    	bdl.putString("operation_type", "single_file");
    	bdl.putString("file_absolute_path", f.getAbsolutePath());
    	opDialog.setArguments(bdl);
		
		opDialog.show(ac.getSupportFragmentManager(), null);
	}
	
	public void openMultipleFilesOperationsDialog() 
	{
		OperationsDialogFragment opDialog = new OperationsDialogFragment();
		
    	Bundle bdl = new Bundle(1);
    	bdl.putString("operation_type", "multiple_files");
    	opDialog.setArguments(bdl);
		
    	opDialog.show(ac.getSupportFragmentManager(), null);
	}
	
	public void openDefaultOperationsDialog() 
	{
		OperationsDialogFragment opDialog = new OperationsDialogFragment();
		
    	Bundle bdl = new Bundle(1);
    	bdl.putString("operation_type", "default");
    	bdl.putString("file_absolute_path", BrowseHandler.current_path);
    	opDialog.setArguments(bdl);
		
    	opDialog.show(ac.getSupportFragmentManager(), null);
	}
	
	//getter/setter
	private static ArrayList<ClipboardFile> clipboard_files = new ArrayList<ClipboardFile>();
	
	public static ArrayList<ClipboardFile> getClipboard_files() {
		return clipboard_files;
	}
	
	public static ArrayList<File> getFilesFromClipboard()
	{
		ArrayList<File> l = new ArrayList<File>();
		
		for(int i = 0; i < clipboard_files.size(); ++i)
			l.add(clipboard_files.get(i).getFile());
		
		return l;
	}


	ClipboardDialogFragment cdf;
	public void openClipboardDialog()
	{
		
		cdf = new ClipboardDialogFragment();
		cdf.show(ac.getSupportFragmentManager(), null);
	}
	
	
	private void showOperationsPickButton()
	{
    	//if invisible, show button at bottom
		Button ops = (Button) ac.findViewById(R.id.operation_pick);
		
		if(ops.getVisibility() == View.GONE) {
			ops.setVisibility(View.VISIBLE);
		}
	}
	
	private void hideOperationsPickButton()
	{
    	//if invisible, show button at bottom
		Button ops = (Button) ac.findViewById(R.id.operation_pick);
		
		if(ops.getVisibility() != View.GONE) {
			ops.setVisibility(View.GONE);
		}
	}
	
	public void cut(File f) 
	{
		showOperationsPickButton();
		
		int index;
		if((index = existsInClipboard(f)) != -1) {
			clipboard_files.get(index).setStatus(ClipboardFile.STATUS_CUT);
		} else { 
			clipboard_files.add(0, new ClipboardFile(f, ClipboardFile.STATUS_CUT));
		}	
	}
	
	public void copy(File f) 
	{
		showOperationsPickButton();
		
		int index;
		if((index = existsInClipboard(f)) != -1) {
			clipboard_files.get(index).setStatus(ClipboardFile.STATUS_COPY);
		} else {
			clipboard_files.add(0, new ClipboardFile(f, ClipboardFile.STATUS_COPY));
		}
		
	}
	
	public static final boolean OP_SINGLE = false;
	public static final boolean OP_MULTIPLE = true;
	public boolean last_operation = OP_SINGLE;
	public boolean getLast_operation() {
		return last_operation;
	}
	public void setLast_operation(boolean last_operation) {
		this.last_operation = last_operation;
	}
	
	//ClipboardFile arg: used to differentiate cut from copy
	public void paste(ClipboardFile source) 
	{

		if(source == null) { //shouldn't happen
			displayOperationMessage("Error: clipboard empty.");
			return;
		}

		File target = new File(BrowseHandler.current_path.concat("/").concat(source.getFile().getName()));
		
		Log.v("Source path:", source.getFile().getAbsolutePath().toString());
		Log.v("Dest path:", target.getAbsolutePath().toString());
		
		if(source.getFile().getAbsolutePath().toString().compareTo(target.getAbsolutePath().toString()) == 0) {
			displayOperationMessage("Invalid path (same).");
			return;
		}

		
		//http://stackoverflow.com/questions/5715104/copy-files-from-a-folder-of-sd-card-into-another-folder-of-sd-card
		if(source.getFile().isDirectory()) {
			try {
				General.copyDirectory(source.getFile(), target);
			} catch(Exception e) {
				displayOperationMessage("Error: copy failed: ".concat(e.getMessage()));
				return;
			}
		} else { 
			try {
				General.copyFile(source.getFile(), target);
			} catch(Exception e) {
				displayOperationMessage("Error: copy failed: ".concat(e.getMessage()));
				return;
			}
		}
		
		clipboard_files.remove(0);

		
		//use the delete() method
		if(source.getStatus() == ClipboardFile.STATUS_CUT)
			delete(source.getFile(), false);

		if(clipboard_files.size() == 0) {
			hideOperationsPickButton();
		}
		
		displayOperationMessage("Paste successfull.");
		cdf.dismiss();
		
		BrowseHandler bh = BrowseHandler.getInstance();
		bh.refreshContent();
	}
	
	public void pasteAll()
	{
		
	}
	
	public void clearClipboard()
	{
		clipboard_files.clear();
		cdf.dismiss();
		hideOperationsPickButton();
	}
	
	
	private int existsInClipboard(File f)
	{
		for(int i = 0; i < clipboard_files.size(); ++i) {    
		    if(f.getAbsolutePath().toString()
		    		.compareTo(clipboard_files.get(i).getFile().getAbsolutePath().toString()) == 0) {
		    	return i;
		    }
		}
		return -1;
	}
	
	private void displayOperationMessage(String message)
	{
		Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
	}
	
	//rename
	public void rename(File f, String new_name) 
	{
		//rename: what if parentFile() is "/"?
		//parent can't be "/", because we don't have permissions to create files/folders at "/"
		if(!f.renameTo(new File(f.getParentFile().getAbsolutePath().concat("/".concat(new_name))))) {
			displayOperationMessage("Rename failed");
			return;
		}
		displayOperationMessage("Rename success.");
	}

	
	//you don't output errors in delete().
	//because e.g. deleteAll might use it: it'll spam then
	boolean deleteErrors;
	public void delete(File f, boolean show_messages)
	{
		deleteRecursive(f);
		
		if(deleteErrors) {
			if(show_messages)
				displayOperationMessage("Errors while deleting.");
		} else {
			if(show_messages)
				displayOperationMessage("Delete success.");
		}
	}
	
	private void deleteRecursive(File fileOrDirectory) {
	    if (fileOrDirectory.isDirectory())
	        for (File child : fileOrDirectory.listFiles())
	            deleteRecursive(child);

	    if(!fileOrDirectory.delete())
	    	deleteErrors = true;
	}

	
	
	//select
	private boolean selectActive = false;
	public boolean isSelectActive() {
		return selectActive;
	}
	public void setSelectActive(boolean selectActive) {
		this.selectActive = selectActive;
	}
	
	private ArrayList<File> selected_files = new ArrayList<File>();
	public ArrayList<File> getSelected_files() {
		return selected_files;
	}
	public void setSelected_files(ArrayList<File> selected_files) {
		this.selected_files = selected_files;
	}
	public void beginSelect()
	{
		//set background
		if(!isSelectActive()) {
			setSelectButtonBackground();
			setSelectActive(true);
			
			//turn on multi-select operations
			//MS operations:
			//- copy, cut, delete, hide, cancel
			
			
			displayOperationMessage("Select ready.");
		}
	}
	
	public void cancelSelect() 
	{
		unsetSelectButtonBackground();
		setSelectActive(false);
		
		selected_files.clear();
		
		BrowseHandler bh = BrowseHandler.getInstance();
		bh.markSelectedFiles();
		
	}
	
	private void setSelectButtonBackground()
	{
		Button btn = (Button) ac.findViewById(R.id.select_button);
		btn.setBackgroundResource(R.layout.gradient_bg);
	}
	private void unsetSelectButtonBackground()
	{
		Button btn = (Button) ac.findViewById(R.id.select_button);
		btn.setBackgroundColor(Color.WHITE);
		
	}
	
	private boolean fileAlreadySelected(File f)
	{
		for(int i = 0; i < selected_files.size(); ++i)
			if(f.getAbsolutePath()
					.equals(selected_files.get(i).getAbsolutePath()))
				return true;
		return false;
	}
	
	
	
	public void selectFile(File f)
	{
		if(fileAlreadySelected(f)) {
			deselectFile(f);
			//selected_files.remove(f);
		} else {
			selected_files.add(f);
		}
	}

	private void deselectFile(File f)
	{
		removeFromSelectedFiles(f);
	}
	
	private void removeFromSelectedFiles(File f)
	{
		for(int i = 0; i < selected_files.size(); ++i) {
			if(f.getName().equals(selected_files.get(i).getName())) {
				selected_files.remove(i);
				return;
			}
		}
	}
	

	
	public void cutSelectedFiles()
	{
		for(int i = 0; i < selected_files.size(); ++i)
			cut(selected_files.get(i));
		
		cancelSelect();
	}
	
	public void copySelectedFiles()
	{
		for(int i = 0; i < selected_files.size(); ++i)
			copy(selected_files.get(i));
		
		cancelSelect();
	}
	
	public void deleteSelectedFiles()
	{
		for(int i = 0; i < selected_files.size(); ++i)
			delete(selected_files.get(i), false);
		
		cancelSelect();
		
		BrowseHandler bh = BrowseHandler.getInstance();
		bh.refreshContent();
	}
	

	public void selectAll()
	{
		beginSelect();
		
		ListView lv = (ListView) ac.findViewById(android.R.id.list);
		MainFileListAdapter adapter = (MainFileListAdapter) lv.getAdapter();
		SelectedFile[] files = adapter.getData();
		
		for(int i = 0; i < files.length; ++i) {
			File f = files[i].getFile();
			
			if(!fileAlreadySelected(f))
				selectFile(f);
		}
		
		BrowseHandler bh = BrowseHandler.getInstance();
		bh.markSelectedFiles();
	}
	
	public void addShortcut(File f) {
	    //Adding shortcut for MainActivity 
	    //on Home screen
	    Intent shortcutIntent = new Intent(ctx,
	            MainActivity.class);
	    
	    
	    shortcutIntent.setAction(Intent.ACTION_MAIN);
	    shortcutIntent.putExtra("shortcut_path", f.getAbsolutePath());
	 
	    Intent addIntent = new Intent();
	    addIntent
	            .putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
	    addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, f.getName());
	    
	    addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
	            Intent.ShortcutIconResource.fromContext(ctx,
	            		BrowseHandler.getFileIconResourceId(f.getAbsolutePath())));
	 
	    addIntent
	            .setAction("com.android.launcher.action.INSTALL_SHORTCUT");
	    
		ctx.sendBroadcast(addIntent);
		
		displayOperationMessage("Shortcut created successfully.");
	}
	
	public void addFavorite(File f)
	{
		FavoritesHandler fh = new FavoritesHandler(ctx);
		
		displayOperationMessage(fh.insertFavorite(f));
	}
	
	public void hideFile(File f)
	{
		HiddenFileHandler hfh = new HiddenFileHandler(ctx);
		
		hfh.insertHiddenFile(f);
		
		//update the assembled list
		HiddenFileHandler.addHiddenFileHash(new File(f.getParent()).getAbsolutePath(), true);
		
		//refresh current content
		BrowseHandler bh = BrowseHandler.getInstance();
		bh.refreshContent();
		
		displayOperationMessage("File hidden");
	}
	
	public void showCreateNewDialog()
	{
		CreateNewDialogFragment df = new CreateNewDialogFragment();
		
		df.show(ac.getSupportFragmentManager(), null);
	}
	
	public void createNew(String type, String file_name)
	{
		String file_path = BrowseHandler.current_path;
		if(General.lastCharOfString(file_path) != '/')
			 file_path = BrowseHandler.current_path.concat("/");
		
		file_path = file_path.concat(file_name);
		File new_file = new File(file_path);
		
		executeCreateNew(type, new_file);

		BrowseHandler bh = BrowseHandler.getInstance();
		bh.refreshContent();
	}
	
	public void executeCreateNew(String type, File new_file)
	{
		if(!new_file.exists()) {
			if(type == "Folder") {
				boolean result = new_file.mkdir();
				if(result) {
					displayOperationMessage("Folder created successfully.");
				} else {
					displayOperationMessage("Folder creation failed.");
				}
			} else {
				try {
					new_file.createNewFile();
					displayOperationMessage("File created successfully.");
				} catch(Exception e) {
					displayOperationMessage("File creation failed.");
				}
			}
		} else {
			displayOperationMessage("File already exists, please specify another name.");
		}
	}
	
	public boolean executeCreateNewWithoutMessages(String type, File new_file)
	{
		if(!new_file.exists()) {
			if(type == "Folder") {
				boolean result = new_file.mkdir();
				if(result) {
					return true;
				} else {
					return false;
				}
			} else {
				try {
					new_file.createNewFile();
					return true;
				} catch(Exception e) {
					return false;
				}
			}
		} else {
			return false;
		}
	}
	
	public void compressFile(File f)
	{
		boolean status; 
		if(f.isDirectory()) {
			DirectoryZip.setSOURCE_FOLDER(f.getAbsolutePath());
			DirectoryZip.setOUTPUT_ZIP_FILE(f.getAbsolutePath().concat(".zip"));
			status = DirectoryZip.main(null);

		} else {
			FileZip.setSOURCE_FILE(f.getAbsolutePath());
			FileZip.setOUTPUT_ZIP_FILE(f.getAbsolutePath().concat(".zip"));
			status = FileZip.main(null);
		}
		if(status) {
			displayOperationMessage("File compressed.");
		} else {
			displayOperationMessage("Error while compressing file.");
		}
		
		BrowseHandler bh = BrowseHandler.getInstance();
		bh.refreshContent();
	}
	
	public void setDirectoryAsHome(File f)
	{
		if(f.isDirectory()) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
			
			Editor editor = prefs.edit();
			editor.putString("home_directory", f.getAbsolutePath());
			editor.commit();
			
			displayOperationMessage("Home altered.");
		} else {
			displayOperationMessage("Must be a directory.");
		}
	}
	
	public void showFileProperties(File f)
	{
		FilePropertiesDialogFragment d = new FilePropertiesDialogFragment();
		
		Bundle bdl = new Bundle(1);
		bdl.putString("file_absolute_path", f.getAbsolutePath());
		d.setArguments(bdl);
		
		d.show(ac.getSupportFragmentManager(), null);
	}
	
}
