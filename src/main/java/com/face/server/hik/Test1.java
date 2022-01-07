package com.face.server.hik;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.text.SimpleDateFormat;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;


public class Test1 {

	static HCNetSDK hCNetSDK = HCNetSDK.INSTANCE;
	static int lUserID = -1;//用户句柄
	static int m_lSetCardCfgHandle = -1; //下发卡长连接句柄
	static int m_lSetFaceCfgHandle = -1; //下发人脸长连接句柄

	static int dwState = -1; //下发卡数据状态
	static int dwFaceState = -1; //下发人脸数据状态
	
	static int iCharEncodeType = 0;//设备字符集
	
	/**
	 * @param args
	 * @throws UnsupportedEncodingException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws UnsupportedEncodingException, InterruptedException  {
		Test1 test = new Test1();
		hCNetSDK.NET_DVR_Init();
		
		test.Login();				//登陆
		Thread.sleep(500);
		
		test.SetCartTemplate(1); //计划模板配置	
		Thread.sleep(500);
		
		test.GetAllCard(); //查询所有卡参数
		Thread.sleep(500);
		
		//下发1张卡号，关联计划模板1
		String strCardNo = "222";
		test.SetOneCard(strCardNo, (short)1);	
		Thread.sleep(500);
		
		//查询指定卡参数		
		test.GetOneCard(strCardNo); 
		Thread.sleep(500);
		
		//查询指定卡号关联的人脸图片
	    strCardNo = "222";
		test.GetFaceCfg(strCardNo); 
		Thread.sleep(500);
		
		//采集一张人脸图片		
		test.GetFaceInfo();			
		
		//下发1张人脸
		test.SetOneFace(strCardNo);		
		Thread.sleep(500);	
		
		//批量下发两个卡参数
		String[] strMultiCardNo = new String[2];
		strMultiCardNo[0] = "1235123";
		strMultiCardNo[1] = "7895987";
		int[] iEmployeeNo = new int[2]; //工号，不同卡号关联工号不能重复
		iEmployeeNo[0] = 21; 
		iEmployeeNo[1] = 32;
		test.SetMultiCard(strMultiCardNo, iEmployeeNo, 2, (short)1);	//批量下发多张卡号，关联计划模板1	
		Thread.sleep(500);	
		
		//批量下发两个卡号关联的人脸
		String[] strFilePath = new String[2];
		strFilePath[0] = System.getProperty("user.dir") + "\\lib\\pic\\face1.jpg";
		strFilePath[1] = System.getProperty("user.dir") + "\\lib\\pic\\face2.jpg";		
		test.SetMultiFace(strMultiCardNo, strFilePath, 2);
		Thread.sleep(500);	
		
		//删除人脸
		test.DelOneFace(strCardNo);  
		Thread.sleep(500);
		
		//删除卡号
		test.DelOneCard(strCardNo); 		
		Thread.sleep(500);  
		
		//退出的时候注销\释放SDK资源
		hCNetSDK.NET_DVR_Logout(lUserID);
		hCNetSDK.NET_DVR_Cleanup();		
	}

	public void Login()
	{			
		//注册
		HCNetSDK.NET_DVR_USER_LOGIN_INFO m_strLoginInfo = new HCNetSDK.NET_DVR_USER_LOGIN_INFO();//设备登录信息	
		
        String m_sDeviceIP = "10.17.36.2";//设备ip地址  
        m_strLoginInfo.sDeviceAddress = new byte[HCNetSDK.NET_DVR_DEV_ADDRESS_MAX_LEN];
        System.arraycopy(m_sDeviceIP.getBytes(), 0, m_strLoginInfo.sDeviceAddress, 0, m_sDeviceIP.length());
        
        String m_sUsername = "admin";//设备用户名
        m_strLoginInfo.sUserName = new byte[HCNetSDK.NET_DVR_LOGIN_USERNAME_MAX_LEN];
        System.arraycopy(m_sUsername.getBytes(), 0, m_strLoginInfo.sUserName, 0, m_sUsername.length());
        
        String m_sPassword = "hik12345";//设备密码
        m_strLoginInfo.sPassword = new byte[HCNetSDK.NET_DVR_LOGIN_PASSWD_MAX_LEN];
        System.arraycopy(m_sPassword.getBytes(), 0, m_strLoginInfo.sPassword, 0, m_sPassword.length());
        
        m_strLoginInfo.wPort = 8000;        
        m_strLoginInfo.bUseAsynLogin = false; //是否异步登录：0- 否，1- 是        
        m_strLoginInfo.write();
        
        HCNetSDK.NET_DVR_DEVICEINFO_V40 m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V40();//设备信息
        lUserID = hCNetSDK.NET_DVR_Login_V40(m_strLoginInfo, m_strDeviceInfo);
        if (lUserID == -1)
		{
			System.out.println("登录失败，错误码为"+hCNetSDK.NET_DVR_GetLastError());
			return;
		} 
		else
		{
			System.out.println("登录成功！");
			iCharEncodeType = m_strDeviceInfo.byCharEncodeType;
		}		
	}
	
	public void GetOneCard(String strCardNo)
	{
		HCNetSDK. NET_DVR_CARD_COND struCardCond = new HCNetSDK.NET_DVR_CARD_COND();
		struCardCond.read();
		struCardCond.dwSize = struCardCond.size();
		struCardCond.dwCardNum = 1; //查询一个卡参数
		struCardCond.write();
		Pointer ptrStruCond = struCardCond.getPointer();	
		
		m_lSetCardCfgHandle = hCNetSDK.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_GET_CARD, ptrStruCond, struCardCond.size(),null ,null);
		if (m_lSetCardCfgHandle == -1)
		{
			System.out.println("建立查询卡参数长连接失败，错误码为"+hCNetSDK.NET_DVR_GetLastError());
			return;
		} 
		else
		{
			System.out.println("建立查询卡参数长连接成功！");
		}
		
		//查找指定卡号的参数，需要下发查找的卡号条件
		HCNetSDK.NET_DVR_CARD_SEND_DATA struCardNo = new HCNetSDK.NET_DVR_CARD_SEND_DATA();
		struCardNo.read();
		struCardNo.dwSize = struCardNo.size();
		
		for (int i = 0; i < HCNetSDK.ACS_CARD_NO_LEN; i++)
        {
			struCardNo.byCardNo[i] = 0;
        }
        for (int i = 0; i <  strCardNo.length(); i++)
        {
        	struCardNo.byCardNo[i] = strCardNo.getBytes()[i];
        }       
        struCardNo.write();
        
        
        HCNetSDK.NET_DVR_CARD_RECORD struCardRecord = new HCNetSDK.NET_DVR_CARD_RECORD();
        struCardRecord.read();
        
        IntByReference pInt = new IntByReference(0);
        
        while(true){
            dwState = hCNetSDK.NET_DVR_SendWithRecvRemoteConfig(m_lSetCardCfgHandle, struCardNo.getPointer(), struCardNo.size(),
            		struCardRecord.getPointer(), struCardRecord.size(), pInt);
            struCardRecord.read();
            if(dwState == -1){
            	System.out.println("NET_DVR_SendWithRecvRemoteConfig查询卡参数调用失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());
            	break;
            }            
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_NEEDWAIT)
            {	
            	System.out.println("配置等待");
            	try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	continue;
            }
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED)
            {
            	System.out.println("获取卡参数失败, 卡号: " + strCardNo);
            	break;
            }
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION)
            {
            	System.out.println("获取卡参数异常, 卡号: " + strCardNo);
            	break;
            }
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS)  
            {
            	try {
            		String strName = "";
            		if((iCharEncodeType == 0) || (iCharEncodeType == 1) || (iCharEncodeType == 2) )
            		{
            			strName = new String(struCardRecord.byName,"GBK").trim();
            		}
            		
            		if(iCharEncodeType == 6)
            		{
            			strName = new String(struCardRecord.byName,"UTF-8").trim();
            		}
            		
					System.out.println("获取卡参数成功, 卡号: " + new String(struCardRecord.byCardNo).trim() 
							+ ", 卡类型：" + struCardRecord.byCardType 
							+ ", 姓名：" + strName);
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	continue;
            } 
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FINISH) {
            	System.out.println("获取卡参数完成");
            	break;            	
            }            
        }
        
        if(!hCNetSDK.NET_DVR_StopRemoteConfig(m_lSetCardCfgHandle)){
        	System.out.println("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());        	
        }
        else{
        	System.out.println("NET_DVR_StopRemoteConfig接口成功");
        }  
	}
	
	public void GetFaceCfg(String strCardNo)
	{
		HCNetSDK.NET_DVR_FACE_COND struFaceCond = new HCNetSDK.NET_DVR_FACE_COND();
		struFaceCond.read();
		struFaceCond.dwSize = struFaceCond.size();
		struFaceCond.dwFaceNum = 1; //查询一个人脸参数
		struFaceCond.dwEnableReaderNo = 1;//读卡器编号
		
		for (int j = 0; j < HCNetSDK.ACS_CARD_NO_LEN; j++)
        {
			struFaceCond.byCardNo[j] = 0;
        }
		System.arraycopy(strCardNo.getBytes(), 0, struFaceCond.byCardNo, 0, strCardNo.getBytes().length);
		
		struFaceCond.write();
		
		int m_lHandle = hCNetSDK.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_GET_FACE, struFaceCond.getPointer(), struFaceCond.size(),null ,null);
		if (m_lHandle == -1)
		{
			System.out.println("建立查询人脸参数长连接失败，错误码为"+hCNetSDK.NET_DVR_GetLastError());
			return;
		} 
		else
		{
			System.out.println("建立查询人脸参数长连接成功！");
		}
		
		//查询结果
		HCNetSDK.NET_DVR_FACE_RECORD struFaceRecord = new HCNetSDK.NET_DVR_FACE_RECORD();
		struFaceRecord.read();		 
        
        while(true){
            dwState = hCNetSDK.NET_DVR_GetNextRemoteConfig(m_lHandle, struFaceRecord.getPointer(), struFaceRecord.size());
            struFaceRecord.read();
            if(dwState == -1){
            	System.out.println("NET_DVR_GetNextRemoteConfig查询人脸调用失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());
            	break;
            }            
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_NEEDWAIT)
            {	
            	System.out.println("查询中，请等待...");
            	try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	continue;
            }
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED)
            {
            	System.out.println("获取人脸参数失败, 卡号: " + strCardNo);
            	break;
            }
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS)  
            {   
            	if ((struFaceRecord.dwFaceLen > 0) && (struFaceRecord.pFaceBuffer != null)) {
                    FileOutputStream fout;
                    try {
                        String filename = System.getProperty("user.dir") + "\\lib\\pic\\" + strCardNo + "_FaceCfg.jpg";
                        fout = new FileOutputStream(filename);
                        //将字节写入文件
                        long offset = 0;
                        ByteBuffer buffers = struFaceRecord.pFaceBuffer.getByteBuffer(offset, struFaceRecord.dwFaceLen);
                        byte[] bytes = new byte[struFaceRecord.dwFaceLen];
                        buffers.rewind();
                        buffers.get(bytes);
                        fout.write(bytes);
                        fout.close();
                        System.out.println("获取人脸参数成功, 卡号: " + strCardNo + ", 图片保存路径: " + filename);
                    } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }                    
                }
            	continue;
            } 
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FINISH) {
            	System.out.println("获取人脸参数完成");
            	break;            	
            }            
        }
        
        if(!hCNetSDK.NET_DVR_StopRemoteConfig(m_lHandle)){
        	System.out.println("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());        	
        }
        else{
        	System.out.println("NET_DVR_StopRemoteConfig接口成功");
        }  
	}
	
	public void GetFaceInfo()
	{
		HCNetSDK.NET_DVR_CAPTURE_FACE_COND struCapCond = new HCNetSDK.NET_DVR_CAPTURE_FACE_COND();
		struCapCond.read();
		struCapCond.dwSize = struCapCond.size();
		struCapCond.write();
		
		int lGetCfgHandle = hCNetSDK.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_CAPTURE_FACE_INFO, struCapCond.getPointer(), struCapCond.size(),null ,null);
		if (lGetCfgHandle == -1)
		{
			System.out.println("建立采集人脸长连接失败，错误码为"+hCNetSDK.NET_DVR_GetLastError());
			return;
		} 
		else
		{
			System.out.println("建立采集人脸长连接成功！");
		}	
        
		//采集的人脸信息
        HCNetSDK.NET_DVR_CAPTURE_FACE_CFG struFaceInfo = new HCNetSDK.NET_DVR_CAPTURE_FACE_CFG();
        struFaceInfo.read();

        while(true){
            dwState = hCNetSDK.NET_DVR_GetNextRemoteConfig(lGetCfgHandle, struFaceInfo.getPointer(), struFaceInfo.size());
            struFaceInfo.read();
            if(dwState == -1){
            	System.out.println("NET_DVR_GetNextRemoteConfig采集人脸失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());
            	break;
            }            
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_NEEDWAIT)
            {	
            	System.out.println("正在采集中,请等待...");
            	try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	continue;
            }
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED)
            {
            	System.out.println("采集人脸失败");
            	break;
            }
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION)
            {
            	//超时时间5秒内设备本地人脸采集失败就会返回失败,连接会断开
            	System.out.println("采集人脸异常, 网络异常导致连接断开 ");
            	break;
            }
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS)  
            {
            	if ((struFaceInfo.dwFacePicSize > 0) && (struFaceInfo.pFacePicBuffer != null)) {
                    SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
                    String newName = sf.format(new Date());
                    FileOutputStream fout;
                    try {
                        String filename = System.getProperty("user.dir") + "\\lib\\pic\\" + newName  + "_capFaceInfo.jpg";
                        fout = new FileOutputStream(filename);
                        //将字节写入文件
                        long offset = 0;
                        ByteBuffer buffers = struFaceInfo.pFacePicBuffer.getByteBuffer(offset, struFaceInfo.dwFacePicSize);
                        byte[] bytes = new byte[struFaceInfo.dwFacePicSize];
                        buffers.rewind();
                        buffers.get(bytes);
                        fout.write(bytes);
                        fout.close();
                        System.out.println("采集人脸成功, 图片保存路径: " + filename);
                    } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }                    
                }
            	continue;
            } 
            else
            {
            	System.out.println("其他异常, dwState: " + dwState);
            	break;
            }
        }
        
        //采集成功之后断开连接、释放资源
        if(!hCNetSDK.NET_DVR_StopRemoteConfig(lGetCfgHandle)){
        	System.out.println("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());        	
        }
        else{
        	System.out.println("NET_DVR_StopRemoteConfig接口成功");
        }  
	}
	
	public void SetCartTemplate(int iPlanTemplateNumber)
	{
		int iErr = 0;
		
		//设置卡权限计划模板参数
        HCNetSDK.NET_DVR_PLAN_TEMPLATE_COND struPlanCond = new HCNetSDK.NET_DVR_PLAN_TEMPLATE_COND();
        struPlanCond.dwSize = struPlanCond.size();
        struPlanCond.dwPlanTemplateNumber = iPlanTemplateNumber;//计划模板编号，从1开始，最大值从门禁能力集获取
        struPlanCond.wLocalControllerID = 0;//就地控制器序号[1,64]，0表示门禁主机
        struPlanCond.write();
        
        HCNetSDK.NET_DVR_PLAN_TEMPLATE struPlanTemCfg = new HCNetSDK.NET_DVR_PLAN_TEMPLATE();
        struPlanTemCfg.dwSize = struPlanTemCfg.size();
        struPlanTemCfg.byEnable =1; //是否使能：0- 否，1- 是 
        struPlanTemCfg.dwWeekPlanNo = 1;//周计划编号，0表示无效 
        struPlanTemCfg.dwHolidayGroupNo[0] = 0;//假日组编号，按值表示，采用紧凑型排列，中间遇到0则后续无效         

        byte[] byTemplateName;
		try {
			byTemplateName = "计划模板名称测试".getBytes("GBK");
			//计划模板名称 
	        for (int i = 0; i < HCNetSDK.NAME_LEN; i++)
	        {
	        	struPlanTemCfg.byTemplateName[i] = 0;
	        }
			System.arraycopy(byTemplateName, 0, struPlanTemCfg.byTemplateName, 0, byTemplateName.length);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		
		struPlanTemCfg.write();
		
        IntByReference pInt = new IntByReference(0);
	    Pointer lpStatusList = pInt.getPointer();
	     
        if (false == hCNetSDK.NET_DVR_SetDeviceConfig(lUserID, HCNetSDK.NET_DVR_SET_CARD_RIGHT_PLAN_TEMPLATE_V50, 1, struPlanCond.getPointer(), struPlanCond.size(), lpStatusList, struPlanTemCfg.getPointer(), struPlanTemCfg.size()))
	    {
            iErr = hCNetSDK.NET_DVR_GetLastError();
            System.out.println("NET_DVR_SET_CARD_RIGHT_PLAN_TEMPLATE_V50失败，错误号：" + iErr);
            return;
        }
        System.out.println("NET_DVR_SET_CARD_RIGHT_PLAN_TEMPLATE_V50成功！");
        
		//获取卡权限周计划参数		 
	    HCNetSDK.NET_DVR_WEEK_PLAN_COND struWeekPlanCond = new HCNetSDK.NET_DVR_WEEK_PLAN_COND();
	    struWeekPlanCond.dwSize = struWeekPlanCond.size();
	    struWeekPlanCond.dwWeekPlanNumber  = 1;
	    struWeekPlanCond.wLocalControllerID = 0;

	    HCNetSDK.NET_DVR_WEEK_PLAN_CFG struWeekPlanCfg = new HCNetSDK.NET_DVR_WEEK_PLAN_CFG();

		struWeekPlanCond.write();
		struWeekPlanCfg.write();

	    Pointer lpCond = struWeekPlanCond.getPointer();
	    Pointer lpInbuferCfg = struWeekPlanCfg.getPointer();

		if (false == hCNetSDK.NET_DVR_GetDeviceConfig(lUserID, HCNetSDK.NET_DVR_GET_CARD_RIGHT_WEEK_PLAN_V50, 1, lpCond, struWeekPlanCond.size(), lpStatusList, lpInbuferCfg, struWeekPlanCfg.size()))
		{
			 iErr = hCNetSDK.NET_DVR_GetLastError();
	         System.out.println("NET_DVR_GET_CARD_RIGHT_WEEK_PLAN_V50失败，错误号：" + iErr);
	         return;
	    }
	    struWeekPlanCfg.read();
	    
	    struWeekPlanCfg.byEnable = 1; //是否使能：0- 否，1- 是 

	    //避免时间段交叉，先初始化
	    for(int i=0;i<7;i++)
	    {
	    	 for(int j=0;j<8;j++)
	    	 {
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[j].byEnable = 0;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[j].struTimeSegment.struBeginTime.byHour = 0;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[j].struTimeSegment.struBeginTime.byMinute = 0;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[j].struTimeSegment.struBeginTime.bySecond = 0;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[j].struTimeSegment.struEndTime.byHour = 0;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[j].struTimeSegment.struEndTime.byMinute = 0;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[j].struTimeSegment.struEndTime.bySecond = 0;
	         }
	    }
	    
	    //一周7天，全天24小时
	    for(int i=0;i<7;i++)
	    {
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[0].byEnable = 1;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[0].struTimeSegment.struBeginTime.byHour = 0;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[0].struTimeSegment.struBeginTime.byMinute = 0;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[0].struTimeSegment.struBeginTime.bySecond = 0;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[0].struTimeSegment.struEndTime.byHour = 24;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[0].struTimeSegment.struEndTime.byMinute = 0;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[0].struTimeSegment.struEndTime.bySecond = 0;
	    }
	    
	    //一周7天，每天设置2个时间段
	    /*for(int i=0;i<7;i++)
	    {
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[0].byEnable = 1;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[0].struTimeSegment.struBeginTime.byHour = 0;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[0].struTimeSegment.struBeginTime.byMinute = 0;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[0].struTimeSegment.struBeginTime.bySecond = 0;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[0].struTimeSegment.struEndTime.byHour = 11;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[0].struTimeSegment.struEndTime.byMinute = 59;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[0].struTimeSegment.struEndTime.bySecond = 59;

	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[1].byEnable = 1;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[1].struTimeSegment.struBeginTime.byHour = 13;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[1].struTimeSegment.struBeginTime.byMinute = 30;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[1].struTimeSegment.struBeginTime.bySecond = 0;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[1].struTimeSegment.struEndTime.byHour = 19;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[1].struTimeSegment.struEndTime.byMinute = 59;
	            struWeekPlanCfg.struPlanCfg[i].struPlanCfgDay[1].struTimeSegment.struEndTime.bySecond = 59;
	    }*/
	    struWeekPlanCfg.write();
	    
	    //设置卡权限周计划参数	
	    if (false == hCNetSDK.NET_DVR_SetDeviceConfig(lUserID, HCNetSDK.NET_DVR_SET_CARD_RIGHT_WEEK_PLAN_V50, 1, lpCond, struWeekPlanCond.size(), lpStatusList, lpInbuferCfg, struWeekPlanCfg.size()))
	    {
	    	iErr = hCNetSDK.NET_DVR_GetLastError();
	    	System.out.println("NET_DVR_SET_CARD_RIGHT_WEEK_PLAN_V50失败，错误号：" + iErr);
	    	return;
		}
	    System.out.println("NET_DVR_SET_CARD_RIGHT_WEEK_PLAN_V50成功！");     
	}
	
	public void GetAllCard()
	{
		HCNetSDK. NET_DVR_CARD_COND struCardCond = new HCNetSDK.NET_DVR_CARD_COND();
		struCardCond.read();
		struCardCond.dwSize = struCardCond.size();
		struCardCond.dwCardNum = 0xffffffff; //查询所有
		struCardCond.write();
		Pointer ptrStruCond = struCardCond.getPointer();	
		
		m_lSetCardCfgHandle = hCNetSDK.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_GET_CARD, ptrStruCond, struCardCond.size(),null ,null);
		if (m_lSetCardCfgHandle == -1)
		{
			System.out.println("建立下发卡长连接失败，错误码为"+hCNetSDK.NET_DVR_GetLastError());
			return;
		} 
		else
		{
			System.out.println("建立下发卡长连接成功！");
		}
        
        HCNetSDK.NET_DVR_CARD_RECORD struCardRecord = new HCNetSDK.NET_DVR_CARD_RECORD();
        struCardRecord.read();
        struCardRecord.dwSize = struCardRecord.size();
        struCardRecord.write();
        
        IntByReference pInt = new IntByReference(0);
        
        while(true){
            dwState = hCNetSDK. NET_DVR_GetNextRemoteConfig(m_lSetCardCfgHandle, struCardRecord.getPointer(), struCardRecord.size());
            struCardRecord.read();
            if(dwState == -1){
            	System.out.println("NET_DVR_SendWithRecvRemoteConfig接口调用失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());
            	break;
            }            
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_NEEDWAIT)
            {	
            	System.out.println("配置等待");
            	try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	continue;
            }
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED)
            {
            	System.out.println("获取卡参数失败");
            	break;
            }
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION)
            {
            	System.out.println("获取卡参数异常");
            	break;
            }
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS)  
            {
            	try {
            		
            		String strName = "";
            		if((iCharEncodeType == 0) || (iCharEncodeType == 1) || (iCharEncodeType == 2) )
            		{
            			strName = new String(struCardRecord.byName,"GBK").trim();
            		}
            		
            		if(iCharEncodeType == 6)
            		{
            			strName = new String(struCardRecord.byName,"UTF-8").trim();
            		}
            		
					System.out.println("获取卡参数成功, 卡号: " + new String(struCardRecord.byCardNo).trim() 
							+ ", 卡类型：" + struCardRecord.byCardType 
							+ ", 姓名：" + strName);
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	continue;
            } 
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FINISH) {
            	System.out.println("获取卡参数完成");
            	break;            	
            }            
        }
        
        if(!hCNetSDK.NET_DVR_StopRemoteConfig(m_lSetCardCfgHandle)){
        	System.out.println("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());        	
        }
        else{
        	System.out.println("NET_DVR_StopRemoteConfig接口成功");
        }  
		
	}
			
	public void SetOneCard(String strCardNo, short wPlanTemplateNumber) throws UnsupportedEncodingException, InterruptedException{
		HCNetSDK.NET_DVR_CARD_COND struCardCond = new HCNetSDK.NET_DVR_CARD_COND();
		struCardCond.read();
		struCardCond.dwSize = struCardCond.size();
		struCardCond.dwCardNum = 1;  //下发一张
		struCardCond.write();
		Pointer ptrStruCond = struCardCond.getPointer();	
		
		m_lSetCardCfgHandle = hCNetSDK.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_SET_CARD, ptrStruCond, struCardCond.size(),null ,null);
		if (m_lSetCardCfgHandle == -1)
		{
			System.out.println("建立下发卡长连接失败，错误码为"+hCNetSDK.NET_DVR_GetLastError());
			return;
		} 
		else
		{
			System.out.println("建立下发卡长连接成功！");
		}
		
		HCNetSDK.NET_DVR_CARD_RECORD struCardRecord = new HCNetSDK.NET_DVR_CARD_RECORD();
		struCardRecord.read();
		struCardRecord.dwSize = struCardRecord.size();
		
		for (int i = 0; i < HCNetSDK.ACS_CARD_NO_LEN; i++)
        {
			struCardRecord.byCardNo[i] = 0;
        }
        for (int i = 0; i <  strCardNo.length(); i++)
        {
        	struCardRecord.byCardNo[i] = strCardNo.getBytes()[i];
        }
        
		struCardRecord.byCardType = 1; //普通卡
		struCardRecord.byLeaderCard = 0; //是否为首卡，0-否，1-是
		struCardRecord.byUserType = 0;
		struCardRecord.byDoorRight[0] = 1; //门1有权限
		struCardRecord.wCardRightPlan[0] = wPlanTemplateNumber;//关联门计划模板，使用了前面配置的计划模板
		
		struCardRecord.struValid.byEnable = 1;    //卡有效期使能，下面是卡有效期从2000-1-1 11:11:11到2030-1-1 11:11:11
		struCardRecord.struValid.struBeginTime.wYear = 2000;
		struCardRecord.struValid.struBeginTime.byMonth = 1;
		struCardRecord.struValid.struBeginTime.byDay = 1;
		struCardRecord.struValid.struBeginTime.byHour = 11;
		struCardRecord.struValid.struBeginTime.byMinute = 11;
		struCardRecord.struValid.struBeginTime.bySecond = 11;
		struCardRecord.struValid.struEndTime.wYear = 2030;
		struCardRecord.struValid.struEndTime.byMonth = 1;
		struCardRecord.struValid.struEndTime.byDay = 1;
		struCardRecord.struValid.struEndTime.byHour = 11;
		struCardRecord.struValid.struEndTime.byMinute = 11;
		struCardRecord.struValid.struEndTime.bySecond = 11;	
		
		struCardRecord.dwEmployeeNo = 66611; //工号
		
		if((iCharEncodeType == 0) || (iCharEncodeType == 1) || (iCharEncodeType == 2) )
		{
			byte[] strCardName = "测试名称中文再长一点".getBytes("GBK");  //姓名
	        for (int i = 0; i < HCNetSDK.NAME_LEN; i++)
	        {
	        	struCardRecord.byName[i] = 0;
	        }
			System.arraycopy(strCardName, 0, struCardRecord.byName, 0, strCardName.length);
		}
		
		if(iCharEncodeType == 6)
		{
			byte[] strCardName = "测试名称中文再长一点".getBytes("UTF-8");  //姓名
	        for (int i = 0; i < HCNetSDK.NAME_LEN; i++)
	        {
	        	struCardRecord.byName[i] = 0;
	        }
			System.arraycopy(strCardName, 0, struCardRecord.byName, 0, strCardName.length);
		}
		
        struCardRecord.write();
        
        HCNetSDK.NET_DVR_CARD_STATUS struCardStatus = new HCNetSDK.NET_DVR_CARD_STATUS();
        struCardStatus.read();
        struCardStatus.dwSize = struCardStatus.size();
        struCardStatus.write();
        
        IntByReference pInt = new IntByReference(0);
        
        while(true){
            dwState = hCNetSDK.NET_DVR_SendWithRecvRemoteConfig(m_lSetCardCfgHandle, struCardRecord.getPointer(), struCardRecord.size(),struCardStatus.getPointer(), struCardStatus.size(),  pInt);
            struCardStatus.read();
            if(dwState == -1){
            	System.out.println("NET_DVR_SendWithRecvRemoteConfig接口调用失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());
            	break;
            }            
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_NEEDWAIT)
            {	
            	System.out.println("配置等待");
            	Thread.sleep(10);
            	continue;
            }
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED)
            {
            	System.out.println("下发卡失败, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 错误码：" + struCardStatus.dwErrorCode);
            	break;
            }
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION)
            {
            	System.out.println("下发卡异常, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 错误码：" + struCardStatus.dwErrorCode);
            	break;
            }
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS)  
            {
            	if (struCardStatus.dwErrorCode != 0){
            		System.out.println("下发卡成功,但是错误码" + struCardStatus.dwErrorCode + ", 卡号：" + new String(struCardStatus.byCardNo).trim());
            	}
            	else{
            		System.out.println("下发卡成功, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 状态：" + struCardStatus.byStatus);
            	} 
            	continue;
            } 
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FINISH) {
            	System.out.println("下发卡完成");
            	break;            	
            }
            
        }
        
        if(!hCNetSDK.NET_DVR_StopRemoteConfig(m_lSetCardCfgHandle)){
        	System.out.println("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());        	
        }
        else{
        	System.out.println("NET_DVR_StopRemoteConfig接口成功");
        }    
        
	}

	public void SetMultiCard(String[] strCardNo, int[] iEmployeeNo, int iNum, short wPlanTemplateNumber) throws UnsupportedEncodingException, InterruptedException{
		HCNetSDK.NET_DVR_CARD_COND struCardCond = new HCNetSDK.NET_DVR_CARD_COND();
		struCardCond.read();
		struCardCond.dwSize = struCardCond.size();
		struCardCond.dwCardNum = iNum;  //下发张数
		struCardCond.write();
		Pointer ptrStruCond = struCardCond.getPointer();	
		
		m_lSetCardCfgHandle = hCNetSDK.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_SET_CARD, ptrStruCond, struCardCond.size(),null ,null);
		if (m_lSetCardCfgHandle == -1)
		{
			System.out.println("建立下发卡长连接失败，错误码为"+hCNetSDK.NET_DVR_GetLastError());
			return;
		} 
		else
		{
			System.out.println("建立下发卡长连接成功！");
		}
		
		HCNetSDK.NET_DVR_CARD_RECORD[] struCardRecord = (HCNetSDK.NET_DVR_CARD_RECORD[])new HCNetSDK.NET_DVR_CARD_RECORD().toArray(iNum);
		
		HCNetSDK.NET_DVR_CARD_STATUS struCardStatus = new HCNetSDK.NET_DVR_CARD_STATUS();
        struCardStatus.read();
        struCardStatus.dwSize = struCardStatus.size();
        struCardStatus.write();
        
        IntByReference pInt = new IntByReference(0);
        
		for(int i =0; i<iNum;i++)
		{
			struCardRecord[i].read();
			struCardRecord[i].dwSize = struCardRecord[i].size();
			
			for (int j = 0; j < HCNetSDK.ACS_CARD_NO_LEN; j++)
	        {
				struCardRecord[i].byCardNo[j] = 0;
	        }
			System.arraycopy(strCardNo[i].getBytes(), 0, struCardRecord[i].byCardNo, 0, strCardNo[i].getBytes().length);

			struCardRecord[i].byCardType = 1; //普通卡
			struCardRecord[i].byLeaderCard = 0; //是否为首卡，0-否，1-是
			struCardRecord[i].byUserType = 0;
			struCardRecord[i].byDoorRight[0] = 1; //门1有权限
			struCardRecord[i].wCardRightPlan[0] = wPlanTemplateNumber;//关联门计划模板，使用了前面配置的计划模板
			
			struCardRecord[i].struValid.byEnable = 1;    //卡有效期使能，下面是卡有效期从2000-1-1 11:11:11到2030-1-1 11:11:11
			struCardRecord[i].struValid.struBeginTime.wYear = 2000;
			struCardRecord[i].struValid.struBeginTime.byMonth = 1;
			struCardRecord[i].struValid.struBeginTime.byDay = 1;
			struCardRecord[i].struValid.struBeginTime.byHour = 11;
			struCardRecord[i].struValid.struBeginTime.byMinute = 11;
			struCardRecord[i].struValid.struBeginTime.bySecond = 11;
			struCardRecord[i].struValid.struEndTime.wYear = 2030;
			struCardRecord[i].struValid.struEndTime.byMonth = 1;
			struCardRecord[i].struValid.struEndTime.byDay = 1;
			struCardRecord[i].struValid.struEndTime.byHour = 11;
			struCardRecord[i].struValid.struEndTime.byMinute = 11;
			struCardRecord[i].struValid.struEndTime.bySecond = 11;	
			
			struCardRecord[i].dwEmployeeNo = iEmployeeNo[i]; //工号
			
			if((iCharEncodeType == 0) || (iCharEncodeType == 1) || (iCharEncodeType == 2) )
			{
				byte[] strCardName = "测试名称中文再长一点".getBytes("GBK");  //姓名
		        for (int j = 0; j < HCNetSDK.NAME_LEN; j++)
		        {
		        	struCardRecord[i].byName[j] = 0;
		        }
				System.arraycopy(strCardName, 0, struCardRecord[i].byName, 0, strCardName.length);
			}
			
			if(iCharEncodeType == 6)
			{
				byte[] strCardName = "测试名称中文再长一点".getBytes("UTF-8");  //姓名
		        for (int j = 0; j < HCNetSDK.NAME_LEN; j++)
		        {
		        	struCardRecord[i].byName[j] = 0;
		        }
				System.arraycopy(strCardName, 0, struCardRecord[i].byName, 0, strCardName.length);
			}
			
	        struCardRecord[i].write();
	        
	        dwState = hCNetSDK.NET_DVR_SendWithRecvRemoteConfig(m_lSetCardCfgHandle, struCardRecord[i].getPointer(), struCardRecord[i].size(),struCardStatus.getPointer(), struCardStatus.size(),  pInt);
            struCardStatus.read();
            if(dwState == -1){
            	System.out.println("NET_DVR_SendWithRecvRemoteConfig接口调用失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());
            }            
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED)
            {
            	System.out.println("下发卡失败, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 错误码：" + struCardStatus.dwErrorCode);
            	//可以继续下发下一个
            }
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION)
            {
            	System.out.println("下发卡异常, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 错误码：" + struCardStatus.dwErrorCode);
            	//异常是长连接异常，不能继续下发后面的数据，需要重新建立长连接
            	break;             	
            }
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS)  
            {
            	if (struCardStatus.dwErrorCode != 0){
            		System.out.println("下发卡失败,错误码" + struCardStatus.dwErrorCode + ", 卡号：" + new String(struCardStatus.byCardNo).trim());
            	}
            	else{
            		System.out.println("下发卡成功, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 状态：" + struCardStatus.byStatus);
            	} 
            	//可以继续下发下一个
            } 
            else
            {
            	System.out.println("其他状态：" + dwState);
            }
		}     
        
        if(!hCNetSDK.NET_DVR_StopRemoteConfig(m_lSetCardCfgHandle)){
        	System.out.println("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());        	
        }
        else{
        	System.out.println("NET_DVR_StopRemoteConfig接口成功");
        }
	}
	
	public void SetOneFace(String strCardNo) throws InterruptedException {
		HCNetSDK.NET_DVR_FACE_COND struFaceCond = new HCNetSDK.NET_DVR_FACE_COND();
		struFaceCond.read();
		struFaceCond.dwSize = struFaceCond.size();
		struFaceCond.byCardNo = "123456".getBytes();
		struFaceCond.dwFaceNum = 1;  //下发一张
		struFaceCond.dwEnableReaderNo = 1;//人脸读卡器编号
		struFaceCond.write();
		Pointer ptrStruFaceCond = struFaceCond.getPointer();	
		
		m_lSetFaceCfgHandle = hCNetSDK.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_SET_FACE, ptrStruFaceCond, struFaceCond.size(),null ,null);
		if (m_lSetFaceCfgHandle == -1)
		{
			System.out.println("建立下发人脸长连接失败，错误码为"+hCNetSDK.NET_DVR_GetLastError());
			return;
		} 
		else
		{
			System.out.println("建立下发人脸长连接成功！");
		}
		
		HCNetSDK.NET_DVR_FACE_RECORD struFaceRecord = new HCNetSDK.NET_DVR_FACE_RECORD();
		struFaceRecord.read();
		struFaceRecord.dwSize = struFaceRecord.size();
		
		for (int i = 0; i < HCNetSDK.ACS_CARD_NO_LEN; i++)
        {
			struFaceRecord.byCardNo[i] = 0;
        }
        for (int i = 0; i <  strCardNo.length(); i++)
        {
        	struFaceRecord.byCardNo[i] = strCardNo.getBytes()[i];
        }
        
        /*****************************************
         * 从本地文件里面读取JPEG图片二进制数据
         *****************************************/
        FileInputStream picfile = null;
        int picdataLength = 0;
        try{
                 picfile = new FileInputStream(new File(System.getProperty("user.dir") + "\\lib\\pic\\face1.jpg"));
                 
        }
        catch(FileNotFoundException e)
        {
        	 e.printStackTrace();
        }

        try{
        	picdataLength = picfile.available();
        }
        catch(IOException e1)
        {
        	e1.printStackTrace();
        }
         if(picdataLength < 0)
        {
        	System.out.println("input file dataSize < 0");
        	return;
        }

        HCNetSDK.BYTE_ARRAY ptrpicByte = new HCNetSDK.BYTE_ARRAY(picdataLength);
        try {
        	picfile.read(ptrpicByte.byValue);
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        ptrpicByte.write();
        struFaceRecord.dwFaceLen  = picdataLength;
        struFaceRecord.pFaceBuffer  = ptrpicByte.getPointer();
		
        struFaceRecord.write();
        
        
        HCNetSDK.NET_DVR_FACE_STATUS struFaceStatus = new HCNetSDK.NET_DVR_FACE_STATUS();
        struFaceStatus.read();
        struFaceStatus.dwSize = struFaceStatus.size();
        struFaceStatus.write();
        
        IntByReference pInt = new IntByReference(0);
        
        while(true){
        	dwFaceState = hCNetSDK.NET_DVR_SendWithRecvRemoteConfig(m_lSetFaceCfgHandle, struFaceRecord.getPointer(), struFaceRecord.size(),struFaceStatus.getPointer(), struFaceStatus.size(),  pInt);
            struFaceStatus.read();
            if(dwFaceState == -1){
            	System.out.println("NET_DVR_SendWithRecvRemoteConfig接口调用失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());
            	break;
            }            
            else if(dwFaceState == HCNetSDK.NET_SDK_CONFIG_STATUS_NEEDWAIT)
            {	
            	System.out.println("配置等待");
            	Thread.sleep(10);
            	continue;
            }
            else if(dwFaceState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED)
            {
            	System.out.println("下发人脸失败, 卡号: " + new String(struFaceStatus.byCardNo).trim() + ", 错误码：" + hCNetSDK.NET_DVR_GetLastError());
            	break;
            }
            else if(dwFaceState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION)
            {
            	System.out.println("下发人脸异常, 卡号: " + new String(struFaceStatus.byCardNo).trim() + ", 错误码：" + hCNetSDK.NET_DVR_GetLastError());
            	break;
            }
            else if(dwFaceState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS)  
            {
            	if (struFaceStatus.byRecvStatus != 1){
            		System.out.println("下发人脸失败，人脸读卡器状态" + struFaceStatus.byRecvStatus + ", 卡号：" + new String(struFaceStatus.byCardNo).trim());
            		break;
            	}
            	else{
            		System.out.println("下发人脸成功, 卡号: " + new String(struFaceStatus.byCardNo).trim() + ", 状态：" + struFaceStatus.byRecvStatus);
            	} 
            	continue;
            } 
            else if(dwFaceState == HCNetSDK.NET_SDK_CONFIG_STATUS_FINISH) {
            	System.out.println("下发人脸完成");
            	break;            	
            }
            
        }
        
        if(!hCNetSDK.NET_DVR_StopRemoteConfig(m_lSetFaceCfgHandle)){
        	System.out.println("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());        	
        }
        else{
        	System.out.println("NET_DVR_StopRemoteConfig接口成功");
        }     
        
	}
	
	public void SetMultiFace(String[] strCardNo, String[] strFilePath, int iNum) throws InterruptedException {
		HCNetSDK.NET_DVR_FACE_COND struFaceCond = new HCNetSDK.NET_DVR_FACE_COND();
		struFaceCond.read();
		struFaceCond.dwSize = struFaceCond.size();
		struFaceCond.byCardNo = new byte[32]; //批量下发，该卡号不需要赋值
		struFaceCond.dwFaceNum = iNum;  //下发个数
		struFaceCond.dwEnableReaderNo = 1;//人脸读卡器编号
		struFaceCond.write();
		Pointer ptrStruFaceCond = struFaceCond.getPointer();	
		
		m_lSetFaceCfgHandle = hCNetSDK.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_SET_FACE, ptrStruFaceCond, struFaceCond.size(),null ,null);
		if (m_lSetFaceCfgHandle == -1)
		{
			System.out.println("建立下发人脸长连接失败，错误码为"+hCNetSDK.NET_DVR_GetLastError());
			return;
		} 
		else
		{
			System.out.println("建立下发人脸长连接成功！");
		}
		
		HCNetSDK.NET_DVR_FACE_STATUS struFaceStatus = new HCNetSDK.NET_DVR_FACE_STATUS();
	    struFaceStatus.read();
	    struFaceStatus.dwSize = struFaceStatus.size();
	    struFaceStatus.write();
	        
	    IntByReference pInt = new IntByReference(0);
		for(int i=0; i<iNum; i++)
		{
			HCNetSDK.NET_DVR_FACE_RECORD struFaceRecord = new HCNetSDK.NET_DVR_FACE_RECORD();
			struFaceRecord.read();
			struFaceRecord.dwSize = struFaceRecord.size();
			
			for (int j = 0; j < HCNetSDK.ACS_CARD_NO_LEN; j++)
	        {
				struFaceRecord.byCardNo[j] = 0;
	        }
	        for (int j = 0; j <  strCardNo[i].length(); j++)
	        {
	        	struFaceRecord.byCardNo[j] = strCardNo[i].getBytes()[j];
	        }
	        
	        /*****************************************
	         * 从本地文件里面读取JPEG图片二进制数据
	         *****************************************/
	        FileInputStream picfile = null;
	        int picdataLength = 0;
	        try{
	                 picfile = new FileInputStream(new File(strFilePath[i]));
	                 
	        }
	        catch(FileNotFoundException e)
	        {
	        	 e.printStackTrace();
	        }

	        try{
	        	picdataLength = picfile.available();
	        }
	        catch(IOException e1)
	        {
	        	e1.printStackTrace();
	        }
	         if(picdataLength < 0)
	        {
	        	System.out.println("input file dataSize < 0");
	        	return;
	        }

	        HCNetSDK.BYTE_ARRAY ptrpicByte = new HCNetSDK.BYTE_ARRAY(picdataLength);
	        try {
	        	picfile.read(ptrpicByte.byValue);
	        } catch (IOException e2) {
	            e2.printStackTrace();
	        }
	        ptrpicByte.write();
	        struFaceRecord.dwFaceLen  = picdataLength;
	        struFaceRecord.pFaceBuffer  = ptrpicByte.getPointer();
			
	        struFaceRecord.write();
	        
	        dwFaceState = hCNetSDK.NET_DVR_SendWithRecvRemoteConfig(m_lSetFaceCfgHandle, struFaceRecord.getPointer(), struFaceRecord.size(),struFaceStatus.getPointer(), struFaceStatus.size(),  pInt);
            struFaceStatus.read();
            if(dwFaceState == -1){
            	System.out.println("NET_DVR_SendWithRecvRemoteConfig接口调用失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());
            }            
            else if(dwFaceState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED)
            {
            	System.out.println("下发人脸失败, 卡号: " + new String(struFaceStatus.byCardNo).trim() + ", 错误码：" + hCNetSDK.NET_DVR_GetLastError());
            	//可以继续下发下一个
            }
            else if(dwFaceState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION)
            {
            	System.out.println("下发人脸异常, 卡号: " + new String(struFaceStatus.byCardNo).trim() + ", 错误码：" + hCNetSDK.NET_DVR_GetLastError());
            	//异常是长连接异常，不能继续下发后面的数据，需要重新建立长连接
            	break;
            }
            else if(dwFaceState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS)  
            {
            	if (struFaceStatus.byRecvStatus != 1){
            		System.out.println("下发人脸失败，人脸读卡器状态" + struFaceStatus.byRecvStatus + ", 卡号：" + new String(struFaceStatus.byCardNo).trim());
            	}
            	else{
            		System.out.println("下发人脸成功, 卡号: " + new String(struFaceStatus.byCardNo).trim() + ", 状态：" + struFaceStatus.byRecvStatus);
            	} 
            	//可以继续下发下一个
            } 
            else
            {
            	System.out.println("其他状态：" + dwState);
            }
		}
        
        if(!hCNetSDK.NET_DVR_StopRemoteConfig(m_lSetFaceCfgHandle)){
        	System.out.println("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());        	
        }
        else{
        	System.out.println("NET_DVR_StopRemoteConfig接口成功");
        }
	}
	
	public void DelOneFace(String strCardNo) throws UnsupportedEncodingException, InterruptedException{
		HCNetSDK.NET_DVR_FACE_PARAM_CTRL struFaceDelCond = new HCNetSDK.NET_DVR_FACE_PARAM_CTRL();
		struFaceDelCond.dwSize = struFaceDelCond.size();
		struFaceDelCond.byMode = 0; //删除方式：0- 按卡号方式删除，1- 按读卡器删除

		struFaceDelCond.struProcessMode.setType(HCNetSDK.NET_DVR_FACE_PARAM_BYCARD.class);
		
		//需要删除人脸关联的卡号
		for (int i = 0; i < HCNetSDK.ACS_CARD_NO_LEN; i++)
	    {
			struFaceDelCond.struProcessMode.struByCard.byCardNo[i] = 0;
	    }
	    System.arraycopy(strCardNo.getBytes(), 0, struFaceDelCond.struProcessMode.struByCard.byCardNo, 0, strCardNo.length());
	        
		struFaceDelCond.struProcessMode.struByCard.byEnableCardReader[0] = 1; //读卡器
		struFaceDelCond.struProcessMode.struByCard.byFaceID[0] = 1; //人脸ID
		struFaceDelCond.write();
	        
		Pointer ptrFaceDelCond = struFaceDelCond.getPointer();	
		
		boolean bRet = hCNetSDK.NET_DVR_RemoteControl(lUserID, HCNetSDK.NET_DVR_DEL_FACE_PARAM_CFG, ptrFaceDelCond, struFaceDelCond.size());
		if (!bRet)
		{
			System.out.println("删除人脸失败，错误码为"+hCNetSDK.NET_DVR_GetLastError());
			return;
		} 
		else
		{
			System.out.println("删除人脸成功！");
		}		         
	}
	
	public void DelOneCard(String strCardNo) throws UnsupportedEncodingException, InterruptedException{
		HCNetSDK.NET_DVR_CARD_COND struCardCond = new HCNetSDK.NET_DVR_CARD_COND();
		struCardCond.read();
		struCardCond.dwSize = struCardCond.size();
		struCardCond.dwCardNum = 1;  //下发一张
		struCardCond.write();
		Pointer ptrStruCond = struCardCond.getPointer();	
		
		m_lSetCardCfgHandle = hCNetSDK.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_DEL_CARD, ptrStruCond, struCardCond.size(),null ,null);
		if (m_lSetCardCfgHandle == -1)
		{
			System.out.println("建立删除卡长连接失败，错误码为"+hCNetSDK.NET_DVR_GetLastError());
			return;
		} 
		else
		{
			System.out.println("建立删除卡长连接成功！");
		}
		
		HCNetSDK.NET_DVR_CARD_SEND_DATA struCardData = new HCNetSDK.NET_DVR_CARD_SEND_DATA();
		struCardData.read();
		struCardData.dwSize = struCardData.size();
		
		for (int i = 0; i < HCNetSDK.ACS_CARD_NO_LEN; i++)
        {
			struCardData.byCardNo[i] = 0;
        }
        for (int i = 0; i <  strCardNo.length(); i++)
        {
        	struCardData.byCardNo[i] = strCardNo.getBytes()[i];
        }       
        struCardData.write();
        
        HCNetSDK.NET_DVR_CARD_STATUS struCardStatus = new HCNetSDK.NET_DVR_CARD_STATUS();
        struCardStatus.read();
        struCardStatus.dwSize = struCardStatus.size();
        struCardStatus.write();
        
        IntByReference pInt = new IntByReference(0);
        
        while(true){
            dwState = hCNetSDK.NET_DVR_SendWithRecvRemoteConfig(m_lSetCardCfgHandle, struCardData.getPointer(), struCardData.size(),struCardStatus.getPointer(), struCardStatus.size(),  pInt);
            struCardStatus.read();
            if(dwState == -1){
            	System.out.println("NET_DVR_SendWithRecvRemoteConfig接口调用失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());
            	break;
            }            
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_NEEDWAIT)
            {	
            	System.out.println("配置等待");
            	Thread.sleep(10);
            	continue;
            }
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED)
            {
            	System.out.println("删除卡失败, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 错误码：" + struCardStatus.dwErrorCode);
            	break;
            }
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION)
            {
            	System.out.println("删除卡异常, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 错误码：" + struCardStatus.dwErrorCode);
            	break;
            }
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS)  
            {
            	if (struCardStatus.dwErrorCode != 0){
            		System.out.println("删除卡成功,但是错误码" + struCardStatus.dwErrorCode + ", 卡号：" + new String(struCardStatus.byCardNo).trim());
            	}
            	else{
            		System.out.println("删除卡成功, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 状态：" + struCardStatus.byStatus);
            	} 
            	continue;
            } 
            else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FINISH) {
            	System.out.println("删除卡完成");
            	break;            	
            }            
        }
        
        if(!hCNetSDK.NET_DVR_StopRemoteConfig(m_lSetCardCfgHandle)){
        	System.out.println("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());        	
        }
        else{
        	System.out.println("NET_DVR_StopRemoteConfig接口成功");
        }             
	}	
	
}//Test1 Class结束
