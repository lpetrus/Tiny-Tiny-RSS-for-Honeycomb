package org.fox.ttrss;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.fox.ttrss.types.Article;
import org.fox.ttrss.types.Attachment;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class ArticleFragment extends Fragment {
	private final String TAG = this.getClass().getSimpleName();

	private SharedPreferences m_prefs;
	private Article m_article;
	private OnlineActivity m_activity;
	//private Article m_nextArticle;
	//private Article m_prevArticle;

	public ArticleFragment() {
		super();
	}
	
	public ArticleFragment(Article article) {
		super();
		
		m_article = article;
	}
	
	private View.OnTouchListener m_gestureListener;
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		/* AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo(); */
		
		switch (item.getItemId()) {
		case R.id.article_link_share:
			if (true) {
				((OnlineActivity) getActivity()).shareArticle(m_article);
			}
			return true;
		case R.id.article_link_copy:
			if (true) {
				((OnlineActivity) getActivity()).copyToClipboard(m_article.link);
			}
			return true;
		default:
			Log.d(TAG, "onContextItemSelected, unhandled id=" + item.getItemId());
			return super.onContextItemSelected(item);
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	    ContextMenuInfo menuInfo) {
		
		getActivity().getMenuInflater().inflate(R.menu.article_link_context_menu, menu);
		menu.setHeaderTitle(m_article.title);
		
		super.onCreateContextMenu(menu, v, menuInfo);		
		
	}
	
	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {    	

		m_activity.setProgressBarVisibility(true);
		
		if (savedInstanceState != null) {
			m_article = savedInstanceState.getParcelable("article");
		}
		
		View view = inflater.inflate(R.layout.article_fragment, container, false);
		
		if (m_article != null) {
			
			TextView title = (TextView)view.findViewById(R.id.title);
			
			if (title != null) {
				
				String titleStr;
				
				if (m_article.title.length() > 200)
					titleStr = m_article.title.substring(0, 200) + "...";
				else
					titleStr = m_article.title;
				
				title.setText(titleStr);
				title.setPaintFlags(title.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
				title.setOnClickListener(new OnClickListener() {					
					@Override
					public void onClick(View v) {
						try {
							Intent intent = new Intent(Intent.ACTION_VIEW, 
									Uri.parse(m_article.link.trim()));
								startActivity(intent);
						} catch (Exception e) {
							e.printStackTrace();
							m_activity.toast(R.string.error_other_error);
						}
					}
				});
				
				registerForContextMenu(title); 
			}
			
			TextView comments = (TextView)view.findViewById(R.id.comments);
			
			if (comments != null) {
				if (m_activity.getApiLevel() >= 4 && m_article.comments_count > 0) {
					String commentsTitle = getString(R.string.article_comments, m_article.comments_count);
					comments.setText(commentsTitle);
					comments.setPaintFlags(title.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
					comments.setOnClickListener(new OnClickListener() {					
						@Override
						public void onClick(View v) {
							try {
								String link = (m_article.comments_link != null && m_article.comments_link.length() > 0) ?
									m_article.comments_link : m_article.link; 
								
								Intent intent = new Intent(Intent.ACTION_VIEW, 
										Uri.parse(link.trim()));
									startActivity(intent);
							} catch (Exception e) {
								e.printStackTrace();
								m_activity.toast(R.string.error_other_error);
							}
						}
					});
					
				} else {
					comments.setVisibility(View.GONE);					
				}
			}
			
			WebView web = (WebView)view.findViewById(R.id.content);
			
			if (web != null) {
				web.setWebChromeClient(new WebChromeClient() {					
					@Override
	                public void onProgressChanged(WebView view, int progress) {
	                	m_activity.setProgress(Math.round(((float)progress / 100f) * 10000));
	                	if (progress == 100) {
	                		m_activity.setProgressBarVisibility(false);
	                	}
	                }
				});
				
				String content;
				String cssOverride = "";
				
				WebSettings ws = web.getSettings();
				ws.setSupportZoom(true);
				ws.setBuiltInZoomControls(true);

				web.getSettings().setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);

				TypedValue tv = new TypedValue();				
			    getActivity().getTheme().resolveAttribute(R.attr.linkColor, tv, true);
			    
			    // prevent flicker in ics
			    if (android.os.Build.VERSION.SDK_INT >= 11) {
			    	web.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
			    }

				if (m_prefs.getString("theme", "THEME_DARK").equals("THEME_DARK")) {
					cssOverride = "body { background : transparent; color : #e0e0e0}";
					//view.setBackgroundColor(android.R.color.black);
					web.setBackgroundColor(getResources().getColor(android.R.color.transparent));
				} else {
					cssOverride = "";
				}
				
				String hexColor = String.format("#%06X", (0xFFFFFF & tv.data));
			    cssOverride += " a:link {color: "+hexColor+";} a:visited { color: "+hexColor+";}";

				String articleContent = m_article.content != null ? m_article.content : "";
				
				Document doc = Jsoup.parse(articleContent);
				
				if (doc != null) {
					// thanks webview for crashing on <video> tag
					Elements videos = doc.select("video");
					
					for (Element video : videos)
						video.remove();
					
					articleContent = doc.toString();
				}
				
				String align = m_prefs.getBoolean("justify_article_text", true) ? "text-align : justify;" : "";
				
				switch (Integer.parseInt(m_prefs.getString("font_size", "0"))) {
				case 0:
					cssOverride += "body { "+align+" font-size : 14px; } ";
					break;
				case 1:
					cssOverride += "body { "+align+" font-size : 18px; } ";
					break;
				case 2:
					cssOverride += "body { "+align+" font-size : 21px; } ";
					break;		
				}
				
				content = 
					"<html>" +
					"<head>" +
					"<meta content=\"text/html; charset=utf-8\" http-equiv=\"content-type\">" +
					"<style type=\"text/css\">" +
					"body { padding : 0px; margin : 0px; }" +
					cssOverride +
					/* "img { max-width : 98%; height : auto; }" + */
					"</style>" +
					"</head>" +
					"<body>" + articleContent;
				
				final Spinner spinner = (Spinner) view.findViewById(R.id.attachments);
				
				if (m_article.attachments != null && m_article.attachments.size() != 0) {
					ArrayList<Attachment> spinnerArray = new ArrayList<Attachment>();
					
					ArrayAdapter<Attachment> spinnerArrayAdapter = new ArrayAdapter<Attachment>(
				            getActivity(), android.R.layout.simple_spinner_item, spinnerArray);
					
					spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

					String flatContent = articleContent.replaceAll("[\r\n]", "");
					boolean hasImages = flatContent.matches(".*?<img[^>+].*?");
					
					for (Attachment a : m_article.attachments) {
						if (a.content_type != null && a.content_url != null) {
							
							try {
								if (a.content_type.indexOf("image") != -1 && 
										(!hasImages || m_article.always_display_attachments)) {
									
									URL url = new URL(a.content_url.trim());
									String strUrl = url.toString().trim();

									content += "<p><img src=\"" + strUrl.replace("\"", "\\\"") + "\"></p>";
								}

							} catch (MalformedURLException e) {
								//
							} catch (Exception e) {
								e.printStackTrace();
							}

							spinnerArray.add(a);
						}					
					}
					
					spinner.setAdapter(spinnerArrayAdapter);
					
					Button attachmentsView = (Button) view.findViewById(R.id.attachment_view);
					
					attachmentsView.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							Attachment attachment = (Attachment) spinner.getSelectedItem();
							
							Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(attachment.content_url));
							startActivity(browserIntent);
						}
					});
					
					Button attachmentsCopy = (Button) view.findViewById(R.id.attachment_copy);
					
					attachmentsCopy.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							Attachment attachment = (Attachment) spinner.getSelectedItem();

							if (attachment != null) {
								m_activity.copyToClipboard(attachment.content_url);
							}
						}
					});

					Button attachmentsShare = (Button) view.findViewById(R.id.attachment_share);
					
					if (!m_activity.isPortrait()) {
						attachmentsShare.setOnClickListener(new OnClickListener() {
							
							@Override
							public void onClick(View v) {
								Attachment attachment = (Attachment) spinner.getSelectedItem();
	
								if (attachment != null) {
									m_activity.shareText(attachment.content_url);
								}
							}
						});
					} else {
						attachmentsShare.setVisibility(View.GONE);
					}

				} else {
					view.findViewById(R.id.attachments_holder).setVisibility(View.GONE);
				}
				
				content += "</body></html>";
					
				try {
					web.loadDataWithBaseURL(null, content, "text/html", "utf-8", null);
				} catch (RuntimeException e) {					
					e.printStackTrace();
				}
				
				if (m_activity.isSmallScreen())
					web.setOnTouchListener(m_gestureListener);
			}
			
			TextView dv = (TextView)view.findViewById(R.id.date);
			
			if (dv != null) {
				Date d = new Date(m_article.updated * 1000L);
				SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy, HH:mm");
				dv.setText(df.format(d));
			}
			
			TextView tagv = (TextView)view.findViewById(R.id.tags);
						
			if (tagv != null) {
				if (m_article.feed_title != null) {
					tagv.setText(m_article.feed_title);
				} else if (m_article.tags != null) {
					String tagsStr = "";
				
					for (String tag : m_article.tags)
						tagsStr += tag + ", ";
					
					tagsStr = tagsStr.replaceAll(", $", "");
				
					tagv.setText(tagsStr);
				} else {
					tagv.setVisibility(View.GONE);
				}
			}			
		} 
		
		return view;    	
	}

	@Override
	public void onDestroy() {
		super.onDestroy();		
	}
	
	@Override
	public void onSaveInstanceState (Bundle out) {		
		super.onSaveInstanceState(out);

		out.putParcelable("article", m_article);
	}

	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);		
		
		m_prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
		m_activity = (OnlineActivity)activity;
		//m_article = m_onlineServices.getSelectedArticle(); 
	}

}
