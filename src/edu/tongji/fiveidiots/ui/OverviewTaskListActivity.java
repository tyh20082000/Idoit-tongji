package edu.tongji.fiveidiots.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.tongji.fiveidiots.R;
import edu.tongji.fiveidiots.ctrl.TaskController;
import edu.tongji.fiveidiots.ctrl.TaskInfo;
import edu.tongji.fiveidiots.util.ActivityUtil;
import edu.tongji.fiveidiots.util.Settings;
import edu.tongji.fiveidiots.util.TimeUtil;

/**
 * 主要负责管理tasks的显示和业务逻辑控制
 * @author Andriy @author IRainbow5
 */
public class OverviewTaskListActivity extends OverviewTagListActivity{

	private ListView taskListView;
	private TaskSheetType currentTaskSheetType = TaskSheetType.TODAY;
	private TaskListAdapter adapter = new TaskListAdapter();

	/**
	 * 用于startActivityForResult与回来的自动刷新
	 */
	private static final int REQUEST_TO_SINGLE_TASK = 100;
	
	//关于对话框
	private Dialog mAboutDialog;
	
	private static enum TaskSheetType {
		TODAY,		//今日
		FUTURE,		//已经确定好开始时间（非今天）
		PERIODIC,	//周期性任务
		POOL,			//收集池
		ALL				//全部
	}
	
	/**
	 * 用于存取task_id的string
	 */
	public static final String TASK_ID_STR = "task_id";
	/**
	 * 用于存取task_name的string
	 */
	public static final String TASK_NAME_STR = "task_name";

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//=====在父类中已经调用过mySetActionBarContentView了=====
        taskListView = (ListView) findViewById(R.id.taskListView);
        //=====设置缓存背景色======
        taskListView.setCacheColorHint(0);

		//关于对话框初始化
		mAboutDialog = new AlertDialog.Builder(this).setTitle(this.getString(R.string.about_dialog_title))
				.setMessage(R.string.about_dialog_content).create();
		
		//=====初始化类型，以待后来刷新列表=====
		currentTaskSheetType = TaskSheetType.TODAY;
		setTitle(this.getString(R.string.today));
	}

	@Override
	protected void onResume() {
		super.onResume();

		//=====每当resume的时候，重获一次数据=====
		//后期可能要异步，不然卡UI
		resetTaskList();
	}

	/**
	 * 因为此activity终将继承于GDActivity，告诉其加载哪个layout
	 */
	@Override
	protected void mySetActionBarContentView() {
		setActionBarContentView(R.layout.taskoverview);
	}
	
	/**
	 * 实现父类抽象方法，在taglist的item被点击后调用
	 */
	@Override
	protected void tagListItemClick(String tag) {
//		adapter.foldAll();
//		
//		this.adapter.fillData(new TaskController(this).SearchTag(tag));
//		this.taskListView.setAdapter(this.adapter);
//		this.taskListView.setOnItemClickListener(this.adapter);
//		this.registerForContextMenu(this.taskListView);
	}

	/**
	 * 刷新任务list的listview
	 */
	private void resetTaskList() {
		TaskController controller = new TaskController(this);
		switch (currentTaskSheetType) {
		case POOL:
			//=====得到所有收集池里的任务===== TODO
			this.adapter.fillData(new ArrayList<TaskInfo>());
			break;

		case TODAY:
			//=====得到所有今日任务=====
			this.adapter.fillData(controller.GetTodayTask(new Date()));
			break;

		case FUTURE:
			//=====得到所有未来任务=====
			this.adapter.fillData(controller.GetFutureTask(new Date()));
			break;

		case PERIODIC:
			//=====得到所有周期性任务=====
			this.adapter.fillData(controller.GetPeriodicTask());
			break;

		case ALL:
			//=====得到所有所有任务=====
			this.adapter.fillData(controller.ShowTaskList());
			break;

		default:
			this.adapter.fillData(new ArrayList<TaskInfo>());
			break;
		}

		this.taskListView.setAdapter(this.adapter);
		this.taskListView.setOnItemClickListener(this.adapter);
		this.registerForContextMenu(this.taskListView);
	}
	
	/**
	 * 用于listview的创建context menu
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.task_list_long_click_menus, menu);
		menu.setHeaderTitle(R.string.TL_longclicked_operations);
	}

	/**
	 * 用于处理listview中的相应，可以得到长按住的那个position
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		boolean handleFinished;
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.TL_longclicked_edit:
			//=====进入TaskDetailActivity，带着task_id=====
			Intent intent = new Intent(this, TaskDetailsActivity.class);
			Bundle bundle = new Bundle();
			bundle.putLong(TASK_ID_STR, adapter.getItem(info.position).getId());
			intent.putExtras(bundle);
			this.startActivityForResult(intent, REQUEST_TO_SINGLE_TASK);
			handleFinished = true;
			break;

		case R.id.TL_longclicked_delete:
			//=====删除某个task，然后刷新表单=====
			AlertDialog.Builder builder = new Builder(this);
			builder.setTitle("警告");
			builder.setMessage("确认要删除吗？");
			builder.setPositiveButton(R.string.Dialog_confirm_text, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					TaskInfo task = adapter.getItem(info.position);
					Toast.makeText(OverviewTaskListActivity.this, "正在删除", Toast.LENGTH_SHORT).show();
					TaskController controller = new TaskController(OverviewTaskListActivity.this);
					controller.RemoveTask(task.getId());
					resetTaskList();
				}
			});
			builder.setNegativeButton(R.string.Dialog_cancel_text, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			builder.create().show();
			handleFinished = true;
			break;
		
		default:
			handleFinished = super.onContextItemSelected(item);
		}
		
		return handleFinished;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_TO_SINGLE_TASK) {
			//=====进入到详细信息界面了，出来了以后刷新一下=====
			resetTaskList();
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * 用来显示task listview的adapter
	 * @author Andriy
	 */
	private class TaskListAdapter extends BaseAdapter implements OnItemClickListener{

		/**
		 * 当前的表单里的内容就存放在这里的，外头不存引用了，全部通过adapter来获取
		 */
		private final List<TaskInfo> tasks = new ArrayList<TaskInfo>();
		
		/**
		 * 将一群tasks放入adapter中，原有的会被清空
		 * @param taskInfos
		 */
		public void fillData(List<TaskInfo> taskInfos) {
			this.tasks.clear();
			this.tasks.addAll(taskInfos);
			this.selectedPos = NOT_SELECTED;
		}
		
		/**
		 * 折叠所有的东西
		 */
		public void foldAll() {
			this.selectedPos = NOT_SELECTED;
			notifyDataSetChanged();
		}
		
		// ===为什么选-2呢，因为后头有一个用（selectedPos+1）来比较，如果是-1，则[0]会中枪===
		private final int NOT_SELECTED = -2;
		/**
		 * 当前选中的item的position，如果==NOT_SELECTED，表示没有选中
		 */
		private int selectedPos = NOT_SELECTED;
		
		@Override
		public int getCount() {
			if (tasks.size() == 0) {
				//=====一个view——无=====
				return 1;
			}
			
			if (selectedPos == NOT_SELECTED) {
				return tasks.size();
			}
			else {
				return tasks.size() + 1;
			}
		}

		@Override
		public TaskInfo getItem(int position) {
			//=====根据listview中的位置获得data list中的位置=====
			if (selectedPos == NOT_SELECTED) {
				return tasks.get(position);
			}
			else {
				if (position <= selectedPos) {
					return tasks.get(position);
				}
				else if (position == selectedPos+1) {
					return tasks.get(selectedPos);
				}
				else {
					return tasks.get(position-1);
				}
			}
		}

		@Override
		public long getItemId(int position) {
			//=====根据数据list中的位置获得listview中的位置=====
			if (selectedPos == NOT_SELECTED) {
				return position;
			}
			else {
				if (position <= selectedPos) {
					return position;
				}
				else {
					return position+1;
				}
			}
		}

		/**
		 * 刷一个brief information的界面
		 * @param task 用此task的信息
		 * @return
		 */
		private View getBriefView(TaskInfo task) {
			View view = LayoutInflater.from(OverviewTaskListActivity.this).inflate(R.layout.tasklist_item_brief, null);
			switch (task.getPriority()) {
			case 0:
				view.setBackgroundResource(R.drawable.high_priority_bg);
				break;
			case 1:
				view.setBackgroundResource(R.drawable.mid_priority_bg);
				break;
			case 2:
				view.setBackgroundResource(R.drawable.low_priority_bg);
				break;
			default:
				break;
			}

			TextView taskNameTextView = (TextView) view.findViewById(R.id.TL_taskNameTextView);
			TextView startTimeTextView = (TextView) view.findViewById(R.id.TL_startTimeTextView);
			TextView leftTimeTextView = (TextView) view.findViewById(R.id.TL_leftTimeTextView);

			//task_name
			taskNameTextView.setText(task.getName());
			//TODO 应该是根据表单不同而显示的，就是有的全天要显示，有的不用（TODAY的不用显示今天全天，FUTURE要显示）
			//start time
			Date startTime = task.getStartTime();
			if (startTime != null) {
				startTimeTextView.setText(TimeUtil.isFullDay(startTime) ? TimeUtil	.parseDate(startTime) : TimeUtil
								.parseDateTime(startTime));
			}
			else {
				startTimeTextView.setText("");
			}
			//deadline
			Date deadline = task.getDeadline();
			if (deadline != null) {
				leftTimeTextView.setText(TimeUtil.isFullDay(deadline) ? TimeUtil.parseDate(deadline) : TimeUtil
								.parseDateTime(deadline));
			}
			else {
				leftTimeTextView.setText("");
			}

			return view;
		}
		
		/**
		 *  刷一个extended information的界面
		 * @param task 用此task的信息
		 * @return
		 */
		private View getExtendedView(final TaskInfo task) {
			View view = LayoutInflater.from(OverviewTaskListActivity.this).inflate(R.layout.tasklist_item_extended, null);

			TextView memoTextView = (TextView) view.findViewById(R.id.TL_memoTextView);
			TextView progressTextView = (TextView) view.findViewById(R.id.TL_progressTextView);
			Button startButton = (Button) view.findViewById(R.id.TL_startButton);
			CheckBox finishBox = (CheckBox) view.findViewById(R.id.TL_finishCheckBox);
			
			//memo
			memoTextView.setText(task.getHint());
			//past / total
			int pastMinutes = task.getUsedTime();
			int totalMinutes = task.getTotalTime();
			String progressMessage = "";
			int duration = new Settings(OverviewTaskListActivity.this).getPomotimerDuration();
			int cycle = pastMinutes / duration;
			progressMessage += (cycle + "");
			if (totalMinutes != -1) {
				int totalCycle = totalMinutes / duration;
				progressMessage += (" (" + totalCycle + ") ");
			}
			progressMessage += "时钟周期";
			progressTextView.setText(progressMessage);
			startButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					//=====进入番茄钟界面，传入TASK_ID和TASK_NAME=====
					Bundle bundle = new Bundle();
					bundle.putLong(TASK_ID_STR, task.getId());
					bundle.putString(TASK_NAME_STR, task.getName());
					ActivityUtil.startActivityWithBundle(OverviewTaskListActivity.this, PomotimerActivity.class, 0, false, bundle);
				}
			});
			finishBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					//=====设置完成=====
					TaskController controller = new TaskController(OverviewTaskListActivity.this);
					task.setStatus(TaskInfo.STATUS_FINISHED);
					controller.ModifyTaskInfo(task.getId(), task);
					Toast.makeText(OverviewTaskListActivity.this, "正在设置完成", Toast.LENGTH_SHORT).show();
					resetTaskList();
				}
			});
			
			return view;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			//如果任务集合为空，则不显示任何任务
			if (tasks.isEmpty()) {
				TextView textView = new TextView(OverviewTaskListActivity.this);
				textView.setText("无");
				return textView;
			}
			
			if (convertView != null) {
				//据说这里要判断、要优化，不知道怎么确认老的view和新的view是同一种类型并且不需要重绘
			}
			
			if (selectedPos == NOT_SELECTED) {
				//=====说明没有选中，每一个都是brief information=====
				TaskInfo task = tasks.get(position);
				convertView = this.getBriefView(task);
			}
			else {
				if (position <= selectedPos) {
					//=====要展示的是selected的之前的部分，直接展示brief即可=====
					TaskInfo task = tasks.get(position);
					convertView = this.getBriefView(task);						
				}
				else if (position == selectedPos+1) {
					//=====就是你了！是extended information=====
					TaskInfo task = tasks.get(selectedPos);
					convertView = this.getExtendedView(task);
				}
				else {
					//=====详细的部分已经过了，直接取x-1的那个task展示brief即可=====
					TaskInfo task = tasks.get(position-1);
					convertView = this.getBriefView(task);
				}
			}
			return convertView;
		}

		/**
		 * 此TaskListAdapter不仅仅管理data source，还处理item click！碉堡了！
		 */
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if (selectedPos == NOT_SELECTED) {
				selectedPos = position;
			}
			else {
				if (position < selectedPos) {
					selectedPos = position;
				}
				else if (position == selectedPos) {
					//=====之前选中，现在就不选中=====
					selectedPos = NOT_SELECTED;
				}
				else if (position == selectedPos+1) {
					//=====其实不用处理extended情况，因为它根本就不会被click到，不知道为什么=_======
					return;
				}
				else {	//position > selectedPos+1
					//=====需要减1的！=====
					selectedPos = position - 1;
				}
			}
			
			//=====通知数据更新了，该刷新界面了=====
			this.notifyDataSetChanged();
		}
	}

	/**
	 * 处理当quickaction item按下时的判断，操作
	 */
	@Override
	protected void handleQuickActionItem(int stringId) {
		switch(stringId)
		{
		//左侧，timeline action bar
		case R.string.today:
			currentTaskSheetType = TaskSheetType.TODAY;
			resetTaskList();
			setTitle(this.getString(stringId));
			break;

		case R.string.future:
			currentTaskSheetType = TaskSheetType.FUTURE;
			resetTaskList();
			setTitle(this.getString(stringId));
			break;

		case R.string.periodic:
			currentTaskSheetType = TaskSheetType.PERIODIC;
			resetTaskList();
			setTitle(this.getString(stringId));
			break;

		case R.string.pool:
			currentTaskSheetType = TaskSheetType.POOL;
			resetTaskList();
			setTitle(this.getString(stringId));
			break;

		case R.string.all:
			currentTaskSheetType = TaskSheetType.ALL;
			resetTaskList();
			setTitle(this.getString(stringId));
			break;
			
		//右侧，More action bar
		case R.string.recommend:
			Bundle bundle = new Bundle();
			TaskInfo task = new TaskController(this).Suggest(new Date(), new Settings(this).getPomotimerDuration());
			if (task == null) {
				Toast.makeText(this, "暂无推荐任务", Toast.LENGTH_SHORT).show();
			}
			else {
				bundle.putLong(TASK_ID_STR, task.getId());
				bundle.putString(TASK_NAME_STR, task.getName());
				ActivityUtil.startActivityWithBundle(OverviewTaskListActivity.this, PomotimerActivity.class, 0, false, bundle);
			}
			break;
		case R.string.settings:
			ActivityUtil.startNewActivity(OverviewTaskListActivity.this, SettingsActivity.class, 0L, false);
			break;
		case R.string.about:
			mAboutDialog.show();
			break;
		case R.string.exit:
			OverviewTaskListActivity.this.finish();
			break;
			
		default:
			break;
		}
	}

}
