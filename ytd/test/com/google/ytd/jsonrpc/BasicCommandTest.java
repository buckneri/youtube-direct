package com.google.ytd.jsonrpc;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.google.ytd.command.GetVideoSubmissions;
import com.google.ytd.dao.VideoSubmissionDao;
import com.google.ytd.model.VideoSubmission;

public class BasicCommandTest {
  @Before
  public void setUp() {
  }

  @Test
  public void testGetSubmissions() throws JSONException {
    final VideoSubmission videoSubmission = new VideoSubmission(1l);
    videoSubmission.setArticleUrl("blah");
    final List<VideoSubmission> submissions = new ArrayList<VideoSubmission>();
    submissions.add(videoSubmission);

    JUnit4Mockery mockery = new JUnit4Mockery();
    final VideoSubmissionDao manager = mockery.mock(VideoSubmissionDao.class);
    mockery.checking(new Expectations() {
      {
        oneOf(manager).getSubmissions(with("created"), with("desc"), with("all"));
        will(returnValue(submissions));
      }
    });

    GetVideoSubmissions command = new GetVideoSubmissions(manager);
    Map<String, String> params = new HashMap<String, String>();
    params.put("sortBy", "created");
    params.put("sortOrder", "desc");
    params.put("filterType", "all");
    params.put("pageIndex", "1");
    params.put("pageSize", "10");
    command.setParams(params);
    JSONObject response = command.execute();
    assertNotNull("JSONObject is null", response);
    assertTrue("result length is zero", response.getInt("totalPages") > 0);
  }
}
