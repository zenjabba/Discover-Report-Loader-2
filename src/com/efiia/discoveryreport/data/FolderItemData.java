/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.efiia.discoveryreport.data;

import java.util.ArrayList;

/**
 *
 * @author larry
 */
public class FolderItemData {

	public ArrayList<FileData> Files;
	public ArrayList<FolderData> Folders;
	public ArrayList<FileData> WebLinks;

	public FolderItemData() {
		Files = new ArrayList<>();
		Folders = new ArrayList<>();
		WebLinks = new ArrayList<>();
	}
	
}
