
package com.linecorp.example.weblogin;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value="/line")
public class LineController
{
	private final static String GRANT_TYPE="authorization_code";
	private final static String CLIENT_ID="1479418979";
	private final static String CLIENT_SECRET="6c4078d3640c369aff2a43600e62586d";
	private final static String DIRECT_URI="https://ibank.klikbca.com";

	private final static String POST_ACCESSTOKEN_URL="https://api.line.me/v1/oauth/accessToken";
	private final static String GET_PROFILE_URL="https://api.line.me/v1/profile";

    @RequestMapping(value="/auth", method=RequestMethod.GET)
    public ResponseEntity<String> auth(
        @RequestParam(value="code", required=false) String aCode,
        @RequestParam(value="state", required=false) String aState,
        @RequestParam(value="errorCode", required=false) String aErrorCode,
        @RequestParam(value="errorMessage", required=false) String aErrorMessage)
    {
        if(aCode!=null)
        {
            System.out.println("Auth success with code: " + aCode + " and state: " + aState);
            try
            {
				// POST to get the access token
				HttpClient client= HttpClientBuilder.create().build();
				HttpPost post=new HttpPost(POST_ACCESSTOKEN_URL);
				post.setHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE);

				List<NameValuePair> urlParams=new ArrayList<NameValuePair>();
				urlParams.add(new BasicNameValuePair("grant_type", GRANT_TYPE));
				urlParams.add(new BasicNameValuePair("code", aCode));
				urlParams.add(new BasicNameValuePair("client_id", CLIENT_ID));
				urlParams.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));
				urlParams.add(new BasicNameValuePair("direct_uri", DIRECT_URI));

				post.setEntity(new UrlEncodedFormEntity(urlParams));

				HttpResponse response=client.execute(post);

				BufferedReader br=new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

				StringBuffer result=new StringBuffer();
				String line="";
				while((line=br.readLine())!=null)
				{
					result.append(line);
				}

				System.out.println("response: " + result.toString());

				// Parsed the string result
				Gson g=new Gson();
				TokenInfo token=g.fromJson(result.toString(), TokenInfo.class);
				System.out.println("access_token: " + token.access_token);

				// GET to get user's profile //
				HttpGet get=new HttpGet(GET_PROFILE_URL);
				get.setHeader("Authorization", "bearer " + token.access_token);

				response=client.execute(get);

				br=new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

				result=new StringBuffer();
				while((line=br.readLine())!=null)
				{
					result.append(line);
				}

				System.out.println("response: " + result.toString());

				g=new Gson();
				ProfileInfo profile=g.fromJson(result.toString(), ProfileInfo.class);
				System.out.println("displayName: " + profile.displayName);

				// show the HTML
				String html=String.format("<head>You are logged-in</head><body><p>Welcome %s!</p><br /><img src=\"%s\" /><br /><p>%s</p></body>",
					profile.displayName,
					profile.pictureUrl,
					profile.statusMessage);

                return new ResponseEntity<String>(html, HttpStatus.OK);

            }
            catch(Exception e)
            {
                System.out.println("Exception raised: " + e.getMessage());
                return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        else if(aErrorCode!=null && aErrorMessage!=null)
        {
            System.out.println("Auth failed with error_code: " + aErrorCode + " and error_message: " + aErrorMessage);
            return new ResponseEntity<String>(HttpStatus.OK);
        }
        else
        {
            System.out.println("No params defined");
            return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
};
