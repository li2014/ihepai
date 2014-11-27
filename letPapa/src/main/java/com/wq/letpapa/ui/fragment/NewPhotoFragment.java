package com.wq.letpapa.ui.fragment;

import java.util.ArrayList;

import net.tsz.afinal.http.AjaxParams;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener2;
import com.handmark.pulltorefresh.library.PullToRefreshBase.State;
import com.handmark.pulltorefresh.library.PullToRefreshGridView;
import com.handmark.pulltorefresh.library.extras.SoundPullEventListener;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.wq.letpapa.R;
import com.wq.letpapa.adapter.PhotoBaseAdapter;
import com.wq.letpapa.adapter.XphotAdapter;
import com.wq.letpapa.bean.XPhotos;
import com.wq.letpapa.ui.base.BaseFragment;
import com.wq.letpapa.utils.JsonUtil;

public class NewPhotoFragment extends BaseFragment {

	/**
	 * 设置不支持上啦加载更多 mPullRefreshGridView .setMode(mPullRefreshGridView.getMode()
	 * == Mode.BOTH ? Mode.PULL_FROM_START : Mode.BOTH);
	 * */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		loadCache();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fm_newphoto_layout, null);
	}

	PullToRefreshGridView mPullRefreshGridView;
	private GridView mGridView;
	int nowpage = 1;// 当前页数
	int allcount = 0;// 一共多少条
	int allpage = 0;// 中共多少页
	ArrayList<XPhotos> beans = new ArrayList<XPhotos>();
	XphotAdapter adapter;

	Handler handle = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case WHAT_PARISE:
				XPhotos bean = (XPhotos) msg.obj;
				sendParise(bean.getId(), bean.getUser().getUid());
				break;
			default:
				break;
			}
		}
	};

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mPullRefreshGridView = (PullToRefreshGridView) view
				.findViewById(R.id.pull_refresh_grid);
		adapter = new XphotAdapter(getActivity(), beans, handle);

		adapter.setTheme(PhotoBaseAdapter.THEME_SHOWICON, false);
		adapter.setThemType(PhotoBaseAdapter.THEME_TYPE_PHOTO, false);

		mGridView = mPullRefreshGridView.getRefreshableView();
		// Set a listener to be invoked when the list should be refreshed.
		mGridView.setAdapter(adapter);
		// 设置滑动暂停加载
		mGridView.setOnScrollListener(new PauseOnScrollListener(
				getImageLoader(), true, true));
		mPullRefreshGridView
				.setOnRefreshListener(new OnRefreshListener2<GridView>() {

					@Override
					public void onPullDownToRefresh(
							PullToRefreshBase<GridView> refreshView) {
						nowpage = 1;
						refrashDate();
					}
					@Override
					public void onPullUpToRefresh(
							PullToRefreshBase<GridView> refreshView) {
						if (nowpage > allpage) {
							mPullRefreshGridView.onRefreshComplete();
							Toast.makeText(getActivity(), "已经到底了", 0).show();
							return;
						}
						refrashDate();
					}
				});

		SoundPullEventListener<GridView> soundListener = new SoundPullEventListener<GridView>(
				getActivity());
		soundListener.addSoundEvent(State.PULL_TO_REFRESH, R.raw.feed_pull_refresh);
		mPullRefreshGridView.setOnPullEventListener(soundListener);

		TextView tv = new TextView(getActivity());
		tv.setGravity(Gravity.CENTER);
		tv.setText("没有数据, 你拉一拉试试骚年...");
		mPullRefreshGridView.setEmptyView(tv);
		nowpage = 1;
		refrashDate();
	}

	public void loadCache() {
		JSONObject object = getCache(getClass());
		if (object != null) {
			JsonUtil.parseXPhotosBeans(beans, object);
			allpage = JsonUtil.getPagenum(object);
		}
	}
	/***
	 * page 否 当前页码 默认1 pagesize 否 每页显示条数 默认5 sex 否 性别 lon 否 经度 lat 否 纬度
	 */
	public void refrashDate() {
		AjaxParams params = new AjaxParams();
		params.put("page", nowpage + "");
		params.put("pagesize", 20 + "");
		params.put("sex", getValue(SP_TYPE, TYPE_ALL) + "");
		sendGet(NEW_HEPAI_LIST + "&" + params.getParamString(), null);
	}

	/**
	 * 赞
	 */
	public void sendParise(long id, String xuid) {
		AjaxParams ajaxParams = new AjaxParams();
		ajaxParams.put("uid", getUid());
		ajaxParams.put("type", "photo");
		ajaxParams.put("xid", id + "");
		ajaxParams.put("xuser_id", xuid);
		sendPost(SEND_PRAISE, ajaxParams);
	}

	@Override
	public void onError(String url, String msg) {
		super.onError(url, msg);
		t(msg);
		mPullRefreshGridView.onRefreshComplete();
	}
	
	@Override
	public void onSuccess(String url, Object obj) {
		super.onSuccess(url, obj);
		System.out.println(obj.toString() + "");
		mPullRefreshGridView.onRefreshComplete();
		if (url.startsWith(NEW_HEPAI_LIST)) {
			if (JsonUtil.isSuccess(obj)) {
				if (nowpage == 1) {
					beans.clear();
				}
				try {
					JSONObject object;
					object = new JSONObject(obj.toString());
					JsonUtil.parseXPhotosBeans(beans, object);
					allcount = JsonUtil.getCount(object);
					allpage = JsonUtil.getPagenum(object);
					if(nowpage==1){
						// 将最数据保存到缓存中
						addCache(getClass(), object);
					}
					
					if (nowpage == allcount) {
						Toast.makeText(getActivity(), "已经到底了", 0).show();
					}
					nowpage++;
					adapter.notifyDataSetChanged();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	public void typeChange() {
		nowpage = 1;
		refrashDate();
	}

}
