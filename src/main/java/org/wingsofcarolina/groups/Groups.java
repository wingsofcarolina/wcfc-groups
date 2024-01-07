package org.wingsofcarolina.groups;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.groups.domain.Member;
import org.wingsofcarolina.groups.http.APIException;
import org.wingsofcarolina.groups.http.GroupsIoService;
import org.wingsofcarolina.groups.server.HttpServer;

public class Groups {
	private static final Logger logger = LoggerFactory.getLogger(Groups.class);
	
	private static final String SAVED_LIST = "./Members-saved.xls";
	private static final String UPDATE_LIST = "./Members-update.xls";
	
	private ArrayList<Member> added = new ArrayList<Member>();
	private ArrayList<Member> removed = new ArrayList<Member>();
			
	public static void main(String[] args) throws Exception {
		logger.info("Starting wcfc-groups server");
		HttpServer server = new HttpServer();
		server.run(args);
	}
}
