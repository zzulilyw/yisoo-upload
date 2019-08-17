package com.yisoo.controller;

import com.yisoo.bean.GroupData;
import com.yisoo.bean.GroupDataTableMsg;
import com.yisoo.bean.GroupInfo;
import com.yisoo.bean.GroupMsg;
import com.yisoo.service.GroupDataService;
import com.yisoo.service.GroupInfoService;
import com.yisoo.util.DownFileStreamUtil;
import com.yisoo.util.ExcelParseUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Date;
import java.util.List;


@Controller
public class GroupDataController {
    @Autowired
    GroupDataService groupDataService;
    @Autowired
    GroupInfoService groupInfoService;

//    查看所有名单
    @RequestMapping(value = "group/view/group",method = RequestMethod.GET)
    @ResponseBody
    public GroupDataTableMsg getGroupLine(
            @RequestParam("yisooid")String yisooid
    ){
        List<GroupData> list =groupDataService.getGroupDataListByYisooId(yisooid);

        GroupDataTableMsg groupDataTableMsg = GroupDataTableMsg.success();
        groupDataTableMsg.setData(list);
        System.out.println(list);
        return groupDataTableMsg;
    }

//    上传名单数据
        @RequestMapping(value = "group/view/del",method = RequestMethod.GET)
        @ResponseBody
        public GroupMsg delGroup(
                @RequestParam("groupid")String groupid
        ){
//            删除groupdata
            groupDataService.delGroupDataByGroupId(groupid);
//            删除groupinfo
            groupInfoService.delGroupDataByGroupId(groupid);

            GroupMsg groupMsg = GroupMsg.getSuccess();
            groupMsg.setResult("200");
            return groupMsg;
        }

//  上传名单数据
    @RequestMapping(value = "group/upload/data",method = RequestMethod.POST)
    @ResponseBody
    public GroupMsg uploadData(
            @RequestParam("yisooid")String yisooid,
            @RequestParam("groupid")String groupid,
            @RequestParam("gname")String gName
    ){
        GroupData groupData = new GroupData();
        groupData.setYisooId(Integer.valueOf(yisooid));
        groupData.setGroupId(Integer.valueOf(groupid));
        groupData.setgName(gName);
        groupDataService.updateGroup(groupData);
//        return null;
        GroupMsg groupMsg = GroupMsg.getSuccess();
        return groupMsg;
    }

//上传名单文件
    @RequestMapping(value = "group/upload/file",method = RequestMethod.POST)
    @ResponseBody
    public GroupMsg uploadFile(MultipartFile file, HttpServletRequest request) throws IOException {
        String path = request.getSession().getServletContext().getRealPath("group-files");
        String fileName = (new Date()).getTime() +"+"+file.getOriginalFilename();
        String filepath = path + "\\" + fileName;
        File temp = new File(path,fileName);

        file.transferTo(temp);


        GroupData groupData = new GroupData();
        groupData.setIsClass((byte) 1);
        groupData.setIsEmail((byte) 1);
        groupData.setIsName((byte) 1);
        groupData.setIsNum((byte) 1);
//        groupData.setYisooId(Integer.valueOf(yisooid));
        groupData.setRootFileName(fileName);
        groupData.setRootFilePath(path);
        groupData.setRootFileType("xlsx");

        FileInputStream fileInputStream = new FileInputStream(temp);
        groupData.setRootFileMd5(DigestUtils.md5Hex(fileInputStream));
        fileInputStream.close();

        groupDataService.addGroupData(groupData);

//        由Md5获取文件的groupid
        Integer groupid  = groupDataService.getGroupidByMd5(groupData.getRootFileMd5()).getGroupId();
        System.out.println(groupid);
//        完成excel文件解析

        List<GroupInfo> groupInfoLists = ExcelParseUtil.getGroupInfoLists(filepath);
        for(GroupInfo item :groupInfoLists){
            item.setGroupId(groupid);
        }

//        数据存入数据库
        for (GroupInfo item : groupInfoLists){
            groupInfoService.addGroupInfo(item);
        }

//        数据返回
        return GroupMsg.getGroupListsSuccess(groupInfoLists,String.valueOf(groupid));
    }



//  下载样例
    @RequestMapping(value = "group/down/example",method = RequestMethod.GET)
    @ResponseBody
    public GroupMsg downExample(@RequestParam("type")String type, HttpServletRequest request) {

        String path = null;
        String filename = null;
        if(type.equals("xlsx")){
            filename = "example.xlsx";
        }

        path = request.getSession().getServletContext().getRealPath("group-files\\example")+"\\"+filename;
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("您要下载的资源已被删除！！");
            GroupMsg groupMsg = GroupMsg.getExamplFileFail(new GroupMsg());
            return groupMsg;
        }

        GroupMsg groupMsg = GroupMsg.getExamplFileSuccess(new GroupMsg());
        groupMsg.setExampleFileName(filename);
        return groupMsg;
    }
//
    //下载样例触发
    @RequestMapping(value = "group/down/example/action",method = RequestMethod.GET)
    @ResponseBody
    public void downExampleAction(@RequestParam("filename")String filename,  HttpServletRequest request, HttpServletResponse response) throws IOException {
        DownFileStreamUtil.actionFileDown("group-files\\example",filename,request,response);
    }
}

