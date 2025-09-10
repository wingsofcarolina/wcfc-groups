package org.wingsofcarolina.groups;

import com.opencsv.CSVReader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

public class MemberListCSV extends MemberReader {

  public MemberListCSV(InputStream is) throws Exception {
    super(is);
  }

  @Override
  public List<String[]> readAllLines(InputStream is) throws Exception {
    try (Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
      try (CSVReader csvReader = new CSVReader(reader)) {
        return csvReader.readAll();
      }
    }
  }
}
