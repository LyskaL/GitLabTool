package com.lgc.gitlabtool.git.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.lgc.gitlabtool.git.entities.Branch;
import com.lgc.gitlabtool.git.entities.Project;
import com.lgc.gitlabtool.git.jgit.BranchType;
import com.lgc.gitlabtool.git.jgit.JGit;
import com.lgc.gitlabtool.git.jgit.JGitStatus;

public class GitServiceImpl implements GitService {

    @Override
    public boolean containsBranches(Project project, List<Branch> branches, boolean isCommon) {
        if (project == null || branches == null) {
            throw new IllegalArgumentException("Wrong parameters for obtaining branches.");
        }
        List<Branch> projectBranches = JGit.getInstance().getBranches(project, BranchType.ALL);

        boolean isContains;
        if (isCommon) {
            isContains = projectBranches.containsAll(branches);
        } else {
            isContains = !Collections.disjoint(projectBranches, branches);
        }

        return isContains;
    }

    @Override
    public Map<Project, JGitStatus> switchTo(List<Project> projects, Branch branch) {
        String selectedBranchName = branch.getBranchName();
        boolean isRemote = branch.getBranchType().equals(BranchType.REMOTE);

        Map<Project, JGitStatus> switchStatuses = new HashMap<>();
        for (Project project : projects) {
            JGitStatus status = JGit.getInstance().switchTo(project, selectedBranchName, isRemote);
            switchStatuses.put(project, status);
        }
        return switchStatuses;
    }

    @Override
    public Map<Project, JGitStatus> createBranch(List<Project> projects, String branchName, boolean force) {
        Map<Project, JGitStatus> statuses = new HashMap<>();
        for (Project project : projects.stream().filter(prj -> prj.isCloned()).collect(Collectors.toList())) {
            statuses.put(project, JGit.getInstance().createBranch(project, branchName, force));
        }
        return statuses;
    }

}
