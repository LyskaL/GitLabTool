package com.lgc.gitlabtool.git.jgit;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.DeleteBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.AbortedByHookException;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.CannotDeleteCurrentBranchException;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.NotMergedException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.attributes.AttributesNodeProvider;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.BaseRepositoryBuilder;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectDatabase;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.ReflogReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.PushResult;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.lgc.gitlabtool.git.entities.Project;
import com.lgc.gitlabtool.git.entities.User;
import com.lgc.gitlabtool.git.listeners.stateListeners.ApplicationState;
import com.lgc.gitlabtool.git.services.BackgroundService;
import com.lgc.gitlabtool.git.services.BackgroundServiceImpl;
import com.lgc.gitlabtool.git.services.ProgressListener;
import com.lgc.gitlabtool.git.ui.javafx.ProgressDialog;
import com.lgc.gitlabtool.git.ui.javafx.listeners.OperationProgressListener;

/**
 * Tests for the JGit class.
 *
 * @author Lyudmila Lyska
 */
public class JGitTest {

    private static final String NAME_BRANCH = "test_name";
    private static final String NAME_TRACKING_BRANCH = "test_tracking_branch";
    private static final String CORRECT_PATH = "/path";
    private static final String fileName = "test";

    @Test(expected = IllegalArgumentException.class)
    public void cloneGroupIncorrectDataExceptionGroupTest() {
        getJGitMock(null).clone(null, CORRECT_PATH, getEmptyOperationProgressListener());
    }

    @Test(expected = IllegalArgumentException.class)
    public void cloneGroupIncorrectDataExceptionPathTest() {
        getJGitMock(null).clone(new ArrayList<>(), null, getEmptyOperationProgressListener());
    }


    @Test
    public void gitcloneRepositoryCorrectDataTest() {
        Repository repo = getRepo("_");
        Git gitMock = new Git (getRepository()) {
            @Override
            public Repository getRepository() {
                return repo;
            }
            @Override
            public void close() {
                //Do nothing
            }
        };
        JGit jgit = new JGit(getBackgroundServiceMock()) {
            @Override
            protected Git tryClone(String linkClone, String localPath)
                    throws InvalidRemoteException, TransportException, GitAPIException {
                return gitMock;
            }
        };

        Assert.assertTrue(jgit.clone(getCorrectProject(2), CORRECT_PATH, getEmptyOperationProgressListener()));
    }

    @Test
    public void gitcloneRepositoryCancelExceptionTest() {
        JGit git = new JGit(getBackgroundServiceMock()) {
            @Override
            protected Git tryClone(String linkClone, String localPath) throws JGitInternalException {
                JGitInternalException cancelException = mock(JGitInternalException.class);
                throw cancelException;
            }
        };
        Assert.assertTrue(git.clone(getProjects(2), CORRECT_PATH, getEmptyOperationProgressListener()));

        git = new JGit(getBackgroundServiceMock()) {
            @Override
            protected Git tryClone(String linkClone, String localPath) throws GitAPIException {
                throw getGitAPIException();
            }
        };
        Assert.assertTrue(git.clone(getProjects(2), CORRECT_PATH, getEmptyOperationProgressListener()));
    }

    @Test
    public void gitStatusCorrectDataTest() {
        Git gitMock = getGitMock();
        StatusCommand statusCommandMock = new StatusCommand(getRepository()) {
            @Override
            public Status call() throws GitAPIException, NoWorkTreeException {
                return mock(Status.class);
            }
        };
        Mockito.when(gitMock.status()).thenReturn(statusCommandMock);

        Assert.assertTrue(getJGitMock(gitMock).getStatusProject(getProject(true)).isPresent());
    }

    @Test
    public void gitStatusIncorrectDataTest() {
        StatusCommand statusCommandMock = new StatusCommand(getRepository()) {
            @Override
            public Status call() throws GitAPIException, NoWorkTreeException {
                return null;
            }
        };
        Git gitMock = getGitMock();
        Mockito.when(gitMock.status()).thenReturn(statusCommandMock);
        Assert.assertFalse(getJGitMock(gitMock).getStatusProject(null).isPresent());
        Assert.assertFalse(getJGitMock(gitMock).getStatusProject(getProject(false)).isPresent());
        Assert.assertFalse(getJGitMock(gitMock).getStatusProject(getProject(true)).isPresent());

        statusCommandMock = new StatusCommand(getRepository()) {
            @Override
            public Status call() throws GitAPIException, NoWorkTreeException {
                throw getGitAPIException();
            }
        };
        Mockito.when(gitMock.status()).thenReturn(statusCommandMock);
        Assert.assertFalse(getJGitMock(gitMock).getStatusProject(getProject(true)).isPresent());

        getProject(false).setClonedStatus(true);
        Assert.assertFalse(getJGitMock(gitMock).getStatusProject(getProject(false)).isPresent());
        getProject(false).setClonedStatus(false);
    }

    @Test
    public void addUntrackedFileForCommitCorrectDataTest() {
        Git gitMock = getGitMock();
        AddCommand addCommandMock = new AddCommand(getRepository()) {
            @Override
            public DirCache call() throws GitAPIException {
                return getDirCache();
            }
        };
        Mockito.when(gitMock.add()).thenReturn(addCommandMock);

        List<String> files = new ArrayList<>();
        files.add("0");
        files.add(null);

        List<String> addedFiles = getJGitMock(gitMock).addUntrackedFilesToIndex(files, getProject(true));
        Assert.assertFalse(addedFiles.isEmpty());
        Assert.assertEquals(files.get(0), addedFiles.get(0));
    }

    @Test
    public void addUntrackedFileToIndexIncorrectTest() throws NoFilepatternException, GitAPIException {
        Git gitMock = getGitMock();
        AddCommand addCommandMock = mock(AddCommand.class);
        Mockito.when(addCommandMock.addFilepattern(fileName)).thenReturn(addCommandMock);
        Mockito.when(addCommandMock.call()).thenThrow(getGitAPIException());
        Mockito.when(gitMock.add()).thenReturn(addCommandMock);

        Assert.assertFalse(getJGitMock(null).addUntrackedFileToIndex(fileName, getProject(true)));
        Assert.assertFalse(getJGitMock(gitMock).addUntrackedFileToIndex(fileName, getProject(true)));
    }

    @Test
    public void addUntrackedFileToIndexCorrectTest() throws NoFilepatternException, GitAPIException {
        Git gitMock = getGitMock();
        AddCommand addCommandMock = mock(AddCommand.class);
        Mockito.when(addCommandMock.addFilepattern(fileName)).thenReturn(addCommandMock);
        Mockito.when(addCommandMock.call()).thenReturn(getDirCache());
        Mockito.when(gitMock.add()).thenReturn(addCommandMock);

        Assert.assertTrue(getJGitMock(gitMock).addUntrackedFileToIndex(fileName, getProject(true)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addUntrackedFileToIndexFileIncorrectException() {
        getJGitMock(null).addUntrackedFileToIndex(null, getProject(true));
    }
    @Test(expected = IllegalArgumentException.class)
    public void addUntrackedFileToIndexProjectIncorrectException() {
        getJGitMock(null).addUntrackedFileToIndex("", null);
    }

    @Test
    public void addUntrackedFileForCommitIncorrectDataTest() {
        Git gitMock = getGitMock();
        AddCommand addCommandMock = new AddCommand(getRepository()) {
            @Override
            public DirCache call() throws GitAPIException, NoFilepatternException {
                throw getGitAPIException();
            }
        };
        Mockito.when(gitMock.add()).thenReturn(addCommandMock);
        Assert.assertTrue(getJGitMock(gitMock).addUntrackedFilesToIndex(new ArrayList<>(), getProject(true)).isEmpty());
        Assert.assertTrue(getJGitMock(null).addUntrackedFilesToIndex(new ArrayList<>(), getProject(false)).isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void addUntrackedFileForCommitProjectIsNullTest() {
        getJGitMock(null).addUntrackedFileToIndex(null, getProject(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addUntrackedFileForCommitCollectionIsNullTest() {
        getJGitMock(null).addUntrackedFilesToIndex(new ArrayList<>(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void pullProjectIsNullTest() {
        getJGitMock(null).pull(null);
    }

    @Test
    public void pullIncorrectDataTest() {
        Assert.assertEquals(getJGitMock(null).pull(getProject(false)), JGitStatus.FAILED);
        Assert.assertEquals(getJGitMock(null).pull(getProject(true)), JGitStatus.FAILED);

        Git gitMock = getGitMock();
        JGit jGitMock = new JGit(mock(BackgroundServiceImpl.class));
        Assert.assertEquals(jGitMock.pull(getProject(true)), JGitStatus.FAILED);

        PullCommand pullCommandMock = new PullCommand(getRepository()) {
            @Override
            public PullResult call() throws GitAPIException, WrongRepositoryStateException,
                    InvalidConfigurationException, DetachedHeadException, InvalidRemoteException, CanceledException,
                    RefNotFoundException, RefNotAdvertisedException, NoHeadException, TransportException {
                throw getGitAPIException();
            }
        };
        Mockito.when(gitMock.pull()).thenReturn(pullCommandMock);
        Assert.assertEquals(getJGitMock(gitMock).pull(getProject(true)), JGitStatus.FAILED);
    }

    @Test
    public void pullCorrectDataTest() {
        Git gitMock = getGitMock();
        PullResult pullResultMock = mock(PullResult.class);
        PullCommand pullCommandMock = new PullCommand(getRepository()) {
            @Override
            public PullResult call() throws GitAPIException, WrongRepositoryStateException,
                    InvalidConfigurationException, DetachedHeadException, InvalidRemoteException, CanceledException,
                    RefNotFoundException, RefNotAdvertisedException, NoHeadException, TransportException {
                return pullResultMock;
            }
        };
        Mockito.when(gitMock.pull()).thenReturn(pullCommandMock);

        MergeResult mergeMock = new MergeResult(new ArrayList<>()) {
            @Override
            public MergeStatus getMergeStatus() {
                return MergeStatus.FAST_FORWARD;
            }
        };
        Mockito.when(pullResultMock.getMergeResult()).thenReturn(mergeMock);
        Assert.assertEquals(getJGitMock(gitMock).pull(getProject(true)), JGitStatus.FAST_FORWARD);
    }

    @Test(expected = IllegalArgumentException.class)
    public void commitMessageIsNullTest() {
        getJGitMock(null).commit(getProjects(), null, false, null, null, null, null, new EmptyListener());
    }

    @Test(expected = IllegalArgumentException.class)
    public void commitMessageIsEmptyTest() {
        getJGitMock(null).commit(getProjects(), "", false, null, null, null, null, new EmptyListener());
    }

    @Test(expected = IllegalArgumentException.class)
    public void commitProjectsIsEmptyTest() {
        getJGitMock(null).commit(new ArrayList<>(), "__", false, null, null, null, null, new EmptyListener());
    }

    @Test(expected = IllegalArgumentException.class)
    public void commitProjectsIsNullTest() {
        getJGitMock(null).commit(null, "__", false, null, null, null, null, new EmptyListener());
    }

    @Test(expected = IllegalArgumentException.class)
    public void commitProjectIsNullTest() {
        getJGitMock(null).commitProject(null, "_", false, null, null, null, null);
    }

    @Test
    public void commitProjectIncorrectDataTest() {
        JGitStatus result = getJGitMock(null).commitProject(getProject(true), "_", false, null, null, null, null);
        Assert.assertEquals(result, JGitStatus.FAILED);

        Git gitMock = getGitMock();
        CommitCommand commitCommand = new CommitCommand(getRepository()) {
            @Override
            public RevCommit call() throws GitAPIException, NoHeadException, NoMessageException, UnmergedPathsException,
                    ConcurrentRefUpdateException, WrongRepositoryStateException, AbortedByHookException {
                throw getGitAPIException();
            }
        };
        Mockito.when(gitMock.commit()).thenReturn(commitCommand);

        result = getJGitMock(gitMock).commitProject(getProject(true), "_", false, null, null, null, null);
        Assert.assertEquals(result, JGitStatus.FAILED);
    }

    @Test
    public void commitProjectCorrectDataTest() {
        Git gitMock = getGitMock();
        CommitCommand commitCommand = new CommitCommand(getRepository()) {
            @Override
            public RevCommit call() throws GitAPIException, NoHeadException, NoMessageException, UnmergedPathsException,
                    ConcurrentRefUpdateException, WrongRepositoryStateException, AbortedByHookException {
                return mock(RevCommit.class);
            }
        };
        Mockito.when(gitMock.commit()).thenReturn(commitCommand);
        JGitStatus result = getJGitMock(gitMock).commitProject(getProject(true), "_", false, "Lyuda", "l@gmail.com",
                "Lyuda", "l@gmail.com");
        Assert.assertEquals(result, JGitStatus.SUCCESSFUL);
    }

    @Test
    public void commitAllProjectsCorrectDataTest() {
        Git gitMock = getGitMock();
        CommitCommand commitCommand = new CommitCommand(getRepository()) {
            @Override
            public RevCommit call() throws GitAPIException, NoHeadException, NoMessageException, UnmergedPathsException,
                    ConcurrentRefUpdateException, WrongRepositoryStateException, AbortedByHookException {
                return mock(RevCommit.class);
            }
        };
        Mockito.when(gitMock.commit()).thenReturn(commitCommand);
        Map<Project, JGitStatus> result = getJGitMock(gitMock).commit(getProjects(), "_", false, "Lyuda", "l@gmail.com", "Lyuda",
                "l@gmail.com", new EmptyListener());

        Assert.assertEquals(getCountCorrectProject(getProjects()), getCountCorrectStatuses(result));
    }

    @Test
    public void commitAllProjectsIncorrectDataTest() {
        Map<Project, JGitStatus> result = getJGitMock(null).commit(getProjects(), "_", false, null, null, null, null, new EmptyListener());
        Assert.assertEquals(result.size(), getCountIncorrectStatuses(result));
    }

    @Test(expected = IllegalArgumentException.class)
    public void pushProjectsIsNullTest() {
        getJGitMock(null).push(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void pushProjectsIsEmptyTest() {
        getJGitMock(null).push(new ArrayList<>(), null);
    }

    @Test
    public void pushIncorrectDataTest() {
        Map<Project, JGitStatus> statuses = getJGitMock(null).push(getProjects(), new EmptyListener());
        Assert.assertEquals(statuses.size(), getCountIncorrectStatuses(statuses));

        Git gitMock = getGitMock();
        PushCommand pushCommandMock = new PushCommand(getRepository()) {
            @Override
            public Iterable<PushResult> call() throws GitAPIException, InvalidRemoteException, TransportException {
                throw getGitAPIException();
            }
        };
        Mockito.when(gitMock.push()).thenReturn(pushCommandMock);
        Map<Project, JGitStatus> results = getJGitMock(gitMock).push(getProjects(), new EmptyListener());
        Assert.assertEquals(results.size(), getCountIncorrectStatuses(results));
    }

    @Test(expected = IllegalArgumentException.class)
    public void pushDataWithNullListenerTest() {
        getJGitMock(null).push(getProjects(), null);
    }

    @Test
    public void pushCorrectDataTest() {
        Git gitMock = getGitMock();
        PushCommand pushCommandMock = new PushCommand(getRepository()) {
            @Override
            public Iterable<PushResult> call() throws GitAPIException, InvalidRemoteException, TransportException {
                return Arrays.asList(mock(PushResult.class));
            }
        };
        Mockito.when(gitMock.push()).thenReturn(pushCommandMock);
        Map<Project, JGitStatus> statuses = getJGitMock(gitMock).push(getProjects(),  new EmptyListener());
        Assert.assertEquals(getCountCorrectStatuses(statuses), getCountCorrectProject(getProjects()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void commitAndPushProjectsIsNullTest() {
        getJGitMock(null).commitAndPush(null, "_", false, null, null, null, null, new EmptyListener());
    }

    @Test(expected = IllegalArgumentException.class)
    public void commitAndPushMessageIsNullTest() {
        getJGitMock(null).commitAndPush(getProjects(), null, false, null, null, null, null, new EmptyListener());
    }

    @Test(expected = IllegalArgumentException.class)
    public void commitAndPushProjectsIsEmptyTest() {
        getJGitMock(null).commitAndPush(new ArrayList<>(), "_", false, null, null, null, null, new EmptyListener());
    }

    @Test(expected = IllegalArgumentException.class)
    public void commitAndPushMessageIsEmptyTest() {
        getJGitMock(null).commitAndPush(getProjects(), "", false, null, null, null, null, new EmptyListener());
    }

    @Test
    public void commitAndPushIncorrectDataTest() {
        Map<Project, JGitStatus> result = getJGitMock(null).commitAndPush(getProjects(), "__", false, null, null, null, null, new EmptyListener());
        Assert.assertEquals(result.size(), getCountIncorrectStatuses(result));
    }

    @Test
    public void commitAndPushCorrectDataTest() {
        Git gitMock = getGitMock();
        CommitCommand commitCommand = new CommitCommand(getRepository()) {
            @Override
            public RevCommit call() throws GitAPIException, NoHeadException, NoMessageException, UnmergedPathsException,
                    ConcurrentRefUpdateException, WrongRepositoryStateException, AbortedByHookException {
                return mock(RevCommit.class);
            }
        };
        Mockito.when(gitMock.commit()).thenReturn(commitCommand);

        PushCommand pushCommandMock = new PushCommand(getRepository()) {
            @Override
            public Iterable<PushResult> call() throws GitAPIException, InvalidRemoteException, TransportException {
                return Arrays.asList(mock(PushResult.class));
            }
        };
        Mockito.when(gitMock.push()).thenReturn(pushCommandMock);
        Map<Project, JGitStatus> result = getJGitMock(gitMock).commitAndPush(getProjects(), "__", false, "Lyuda", "l@gmail.com",
                "Lyuda", "l@gmail.com", new EmptyListener());

        Assert.assertEquals(getCountCorrectProject(getProjects()), getCountCorrectStatuses(result));
    }

    @Test(expected = IllegalArgumentException.class)
    public void createBranchProjectsIsNullTest() {
        getJGitMock(null).createBranch(null, "__", null, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createBranchNameBranchIsNullTest() {
        getJGitMock(null).createBranch(new Project(), null, null, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createBranchNameBranchIsEmptyTest() {
        getJGitMock(null).createBranch(new Project(), "", null, false);
    }

    @Test
    public void createBranchIncorrectDataTest() {
        Assert.assertEquals(getJGitMock(null).createBranch(new Project(), "__", null, false), JGitStatus.FAILED);
        Assert.assertEquals(getJGitMock(null).createBranch(getProject(true), "__", null, false), JGitStatus.FAILED);

        Git gitMock = getGitMock();
        ListBranchCommand listCommandMock = new ListBranchCommand(getRepository()) {
            @Override
            public List<Ref> call() throws GitAPIException {
                throw getGitAPIException();
            }

        };
        Mockito.when(gitMock.branchList()).thenReturn(listCommandMock);
        Assert.assertEquals(getJGitMock(gitMock).createBranch(getProject(true), NAME_BRANCH, null, false), JGitStatus.FAILED);

        Ref refMock = mock(Ref.class);
        listCommandMock = getListCommandMock(refMock);
        Mockito.when(refMock.getName()).thenReturn(Constants.R_REMOTES + NAME_BRANCH);
        Mockito.when(gitMock.branchList()).thenReturn(listCommandMock);
        System.err.println("NAME MY BRANCH " + refMock.getName());
        Assert.assertEquals(getJGitMock(gitMock).createBranch(getProject(true), NAME_BRANCH, null, false),
                JGitStatus.BRANCH_ALREADY_EXISTS);

        CreateBranchCommand createBranchCommandMock = new CreateBranchCommand(getRepository()) {
            @Override
            public Ref call()
                    throws GitAPIException, RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException {
                throw getGitAPIException();
            }
        };
        Mockito.when(refMock.toString()).thenReturn(Constants.R_HEADS);
        Mockito.when(refMock.getName()).thenReturn(Constants.R_HEADS + "Test");
        Mockito.when(gitMock.branchCreate()).thenReturn(createBranchCommandMock);
        Assert.assertEquals(getJGitMock(gitMock).createBranch(getProject(true), NAME_BRANCH, null, false), JGitStatus.FAILED);
    }

    @Test
    public void createBranchCorrectDataTest() {
        Ref refMock = mock(Ref.class);
        Git gitMock = getGitMock();
        ListBranchCommand listCommandMock = getListCommandMock(refMock);
        Mockito.when(gitMock.branchList()).thenReturn(listCommandMock);
        CreateBranchCommand createBranchCommandMock = new CreateBranchCommand(getRepository()) {
            @Override
            public Ref call()
                    throws GitAPIException, RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException {
                return refMock;
            }
        };
        Mockito.when(refMock.toString()).thenReturn(Constants.R_HEADS);
        Mockito.when(refMock.getName()).thenReturn(Constants.R_HEADS + "Test");
        Mockito.when(gitMock.branchCreate()).thenReturn(createBranchCommandMock);
        Assert.assertEquals(getJGitMock(gitMock).createBranch(getProject(true), NAME_BRANCH, null, true),
                JGitStatus.SUCCESSFUL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getBranchesProjectNullTest() {
        getJGitMock(null).getBranches(null, BranchType.LOCAL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getBranchesBranchTypeIsNullTest() {
        getJGitMock(null).getBranches(new Project(), null);
    }

    @Test
    public void getBranchesCorrectData() {
        Ref refMock = mock(Ref.class);
        Git gitMock = getGitMock();
        ListBranchCommand listCommandMock = getListCommandMock(refMock);
        Mockito.when(refMock.toString()).thenReturn(Constants.R_HEADS);
        Mockito.when(refMock.getName()).thenReturn(Constants.R_HEADS + "Test");
        Mockito.when(gitMock.branchList()).thenReturn(listCommandMock);

        Assert.assertFalse(getJGitMock(gitMock).getBranches(getProject(true), BranchType.LOCAL).isEmpty());
        Assert.assertFalse(getJGitMock(gitMock).getBranches(getProject(true), BranchType.REMOTE).isEmpty());
    }

    @Test
    public void getBranchesIncorrectData() {
        Git gitMock = getGitMock();
        Assert.assertTrue(getJGitMock(null).getBranches(getProject(true), BranchType.REMOTE).isEmpty());
        Assert.assertTrue(getJGitMock(gitMock).getBranches(getProject(false), BranchType.REMOTE).isEmpty());

    }

    @Test(expected = IllegalArgumentException.class)
    public void getBranchesWithParametersProjectNullTest() {
        getJGitMock(null).getBranches(null, BranchType.LOCAL, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getBranchesWithParametersBranchTypeIsNullTest() {
        getJGitMock(null).getBranches(new ArrayList<>(), null, false);
    }

    @Test
    public void getBranchesWithParametersCorrectData() {
        Assert.assertTrue(getJGitMock(null).getBranches(new ArrayList<>(), BranchType.LOCAL, false).isEmpty());
        Assert.assertTrue(getJGitMock(null).getBranches(new ArrayList<>(), BranchType.REMOTE, false).isEmpty());

        Ref refMock = mock(Ref.class);
        Git gitMock = getGitMock();
        ListBranchCommand listCommandMock = getListCommandMock(refMock);
        Mockito.when(refMock.toString()).thenReturn(Constants.R_HEADS);
        Mockito.when(refMock.getName()).thenReturn(Constants.R_HEADS + "Test");
        Mockito.when(gitMock.branchList()).thenReturn(listCommandMock);

        Assert.assertFalse(getJGitMock(gitMock).getBranches(getProjects(), BranchType.LOCAL, true).isEmpty());
        Assert.assertFalse(getJGitMock(gitMock).getBranches(getProjects(), BranchType.ALL, false).isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getCurrentBranchProjectIsNullTest() {
        getJGitMock(null).getCurrentBranch(null);
    }

    @Test
    public void getCurrentBranchIncorrectData() {
        Assert.assertFalse(getJGitMock(null).getCurrentBranch(getProject(false)).isPresent());
        Assert.assertFalse(getJGitMock(null).getCurrentBranch(getProject(true)).isPresent());

        Git gitMock = getGitMock();
        Repository repoMock = getRepo(null);
        Mockito.when(gitMock.getRepository()).thenReturn(repoMock);
        Assert.assertFalse(getJGitMock(gitMock).getCurrentBranch(getProject(true)).isPresent());
    }

    @Test
    public void getCurrentBranchCorrectData() {
        Git gitMock = getGitMock();
        Repository repoMock = getRepo(NAME_BRANCH);
        Mockito.when(gitMock.getRepository()).thenReturn(repoMock);
        Assert.assertTrue(getJGitMock(gitMock).getCurrentBranch(getProject(true)).isPresent());
    }

    @Test
    public void getTrackingBranchTest() {
        Git gitMock = getGitMock();
        Repository repoMock = getRepo(NAME_BRANCH);
        Mockito.when(gitMock.getRepository()).thenReturn(repoMock);
        Assert.assertTrue(getJGitMock(gitMock).getTrackingBranch(getProject(true)) != null);
        Assert.assertFalse(getJGitMock(gitMock).getTrackingBranch(getProject(true)).isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteBranchProjectIsNullTest() {
        getJGitMock(null).deleteBranch(null, "", false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteBranchNameBranchIsNullTest() {
        getJGitMock(null).deleteBranch(getProject(false), null, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteBranchNameBranchIsEmptyTest() {
        getJGitMock(null).deleteBranch(getProject(false), "", false);
    }

    @Test
    public void deleteBranchIncorrectDataTest() {
        Assert.assertEquals(getJGitMock(null).deleteBranch(getProject(false), NAME_BRANCH, false), JGitStatus.FAILED);
        Assert.assertEquals(getJGitMock(null).deleteBranch(getProject(true), NAME_BRANCH, false), JGitStatus.FAILED);

        Git gitMock = getGitMock();
        Repository repoMock = getRepo(NAME_BRANCH);
        Mockito.when(gitMock.getRepository()).thenReturn(repoMock);
        Assert.assertEquals(getJGitMock(gitMock).deleteBranch(getProject(true), NAME_BRANCH, false), JGitStatus.FAILED);

        DeleteBranchCommand deleteBranchMock = new DeleteBranchCommand(getRepository()) {
            @Override
            public DeleteBranchCommand setBranchNames(String... branchnames) {
                return super.setBranchNames(NAME_BRANCH);
            }

            @Override
            public List<String> call() throws GitAPIException, NotMergedException, CannotDeleteCurrentBranchException {
                throw getGitAPIException();
            }
        };
        repoMock = getRepo(null);
        Mockito.when(gitMock.getRepository()).thenReturn(repoMock);
        Mockito.when(gitMock.branchDelete()).thenReturn(deleteBranchMock);
        Assert.assertEquals(getJGitMock(gitMock).deleteBranch(getProject(true), NAME_BRANCH, false), JGitStatus.FAILED);
    }

    @Test
    public void deleteBranchCorrectDataTest() {
        Git gitMock = getGitMock();
        Repository repoMock = getRepo(null);
        Mockito.when(gitMock.getRepository()).thenReturn(repoMock);
        DeleteBranchCommand deleteBranchMock = new DeleteBranchCommand(getRepository()) {
            @Override
            public DeleteBranchCommand setBranchNames(String... branchnames) {
                return super.setBranchNames(NAME_BRANCH);
            }

            @Override
            public List<String> call() throws GitAPIException, NotMergedException, CannotDeleteCurrentBranchException {
                return Collections.emptyList();
            }
        };
        Mockito.when(gitMock.getRepository()).thenReturn(repoMock);
        Mockito.when(gitMock.branchDelete()).thenReturn(deleteBranchMock);
        Assert.assertEquals(getJGitMock(gitMock).deleteBranch(getProject(true), NAME_BRANCH, false),
                JGitStatus.SUCCESSFUL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkoutBranchProjectIsNullTest() {
        getJGitMock(null).checkoutBranch(null, "__", false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkoutBranchNameIsNullTest() {
        getJGitMock(null).checkoutBranch(getProject(false), null, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkoutBranchNameIsEmptyTest() {
        getJGitMock(null).checkoutBranch(getProject(true), "", false);
    }

    @Test
    public void checkoutBranchIncorrectDataTest() {
        Assert.assertEquals(getJGitMock(null).checkoutBranch(getProject(false), NAME_BRANCH, false), JGitStatus.FAILED);
        //Assert.assertEquals(getJGitMock(null).switchTo(getProject(true), NAME_BRANCH, false), JGitStatus.FAILED);

        Ref refMock = mock(Ref.class);
        Git gitMock = getGitMock();
        ListBranchCommand listCommandMock = getListCommandMock(refMock);
        Mockito.when(refMock.toString()).thenReturn(Constants.R_HEADS);
        Mockito.when(refMock.getName()).thenReturn(Constants.R_HEADS + "Test");
        Mockito.when(gitMock.branchList()).thenReturn(listCommandMock);

        Repository repoMock = getRepo(NAME_BRANCH);
        Mockito.when(gitMock.getRepository()).thenReturn(repoMock);
        Assert.assertEquals(getJGitMock(gitMock).checkoutBranch(getProject(true), NAME_BRANCH, false),
                JGitStatus.BRANCH_DOES_NOT_EXIST);
        Assert.assertEquals(getJGitMock(gitMock).checkoutBranch(getProject(true), NAME_BRANCH, true),
                JGitStatus.BRANCH_CURRENTLY_CHECKED_OUT);

        listCommandMock = getListCommandMock(refMock);
        Mockito.when(refMock.getName()).thenReturn(Constants.R_HEADS + NAME_BRANCH);
        Assert.assertEquals(getJGitMock(gitMock).checkoutBranch(getProject(true), NAME_BRANCH, true),
                JGitStatus.BRANCH_ALREADY_EXISTS);

        Mockito.when(refMock.getName()).thenReturn(Constants.R_HEADS + NAME_BRANCH);

        JGit git = new JGit(getBackgroundServiceMock()) {

            @Override
            protected Git getGit(String path) throws IOException {
                return gitMock;
            }

            @Override
            protected boolean isConflictsBetweenTwoBranches(Repository repo, String firstBranch, String secondBranch) {
                return true;
            }
        };
        Assert.assertEquals(git.checkoutBranch(getProject(true), NAME_BRANCH + "2", true), JGitStatus.CONFLICTS);

        git = new JGit(getBackgroundServiceMock()) {
            @Override
            protected Git getGit(String path) throws IOException {
                return gitMock;
            }

            @Override
            protected boolean isConflictsBetweenTwoBranches(Repository repo, String firstBranch, String secondBranch) {
                return false;
            }
        };

        CheckoutCommand checkoutCommandMock = new CheckoutCommand(getRepository()) {
            @Override
            public Ref call() throws GitAPIException, RefAlreadyExistsException, RefNotFoundException,
                    InvalidRefNameException, CheckoutConflictException {
                throw getGitAPIException();
            }
        };
        Mockito.when(gitMock.checkout()).thenReturn(checkoutCommandMock);
        Assert.assertEquals(git.checkoutBranch(getProject(true), NAME_BRANCH + "2", true), JGitStatus.FAILED);
    }

    @Test
    public void checkoutBranchCorrectDataTest() {
        Git gitMock = getGitMock();
        JGit git = new JGit(getBackgroundServiceMock()) {
            @Override
            protected Git getGit(String path) throws IOException {
                return gitMock;
            }

            @Override
            protected boolean isConflictsBetweenTwoBranches(Repository repo, String firstBranch, String secondBranch) {
                return false;
            }
        };
        Repository repoMock = getRepo(NAME_BRANCH);
        Mockito.when(gitMock.getRepository()).thenReturn(repoMock);

        Ref refMock = mock(Ref.class);
        Mockito.when(refMock.getName()).thenReturn(Constants.R_HEADS + NAME_BRANCH);
        Mockito.when(refMock.toString()).thenReturn(Constants.R_HEADS);

        ListBranchCommand listCommandMock = getListCommandMock(refMock);
        Mockito.when(gitMock.branchList()).thenReturn(listCommandMock);

        CheckoutCommand checkoutCommandMock = new CheckoutCommand(getRepository()) {
            @Override
            public Ref call() throws GitAPIException, RefAlreadyExistsException, RefNotFoundException,
                    InvalidRefNameException, CheckoutConflictException {
                return refMock;
            }
        };
        Mockito.when(gitMock.checkout()).thenReturn(checkoutCommandMock);
        Assert.assertEquals(git.checkoutBranch(getProject(true), NAME_BRANCH + "2", true), JGitStatus.SUCCESSFUL);
    }

    @Test
    public void isConflictsBetweenTwoBranchesInorrectDataTest() {
        Repository repoMock = getRepo(null);
        Assert.assertFalse(getJGitMock(null).isConflictsBetweenTwoBranches(repoMock, "", ""));

        repoMock = getRepo(NAME_BRANCH);
        RevWalk revWalkMockException = new RevWalk(getRepository()) {
            @Override
            public void close() {}

            @Override
            public RevCommit parseCommit(AnyObjectId id)
                    throws MissingObjectException, IncorrectObjectTypeException, IOException {
                throw mock(IOException.class);
            }
        };
        JGit gitException = new JGit(getBackgroundServiceMock()) {
            @Override
            RevWalk getRevWalk(Repository repo) {
                return revWalkMockException;
            }
        };
        Assert.assertFalse(gitException.isConflictsBetweenTwoBranches(repoMock, "", ""));

        RevWalk revWalkMockNull = new RevWalk(getRepository()) {
            @Override
            public void close() {}

            @Override
            public RevCommit parseCommit(AnyObjectId id)
                    throws MissingObjectException, IncorrectObjectTypeException, IOException {
                return null;
            }
        };
        JGit gitNull = new JGit(getBackgroundServiceMock()) {
            @Override
            RevWalk getRevWalk(Repository repo) {
                return revWalkMockNull;
            }
        };
        Assert.assertFalse(gitNull.isConflictsBetweenTwoBranches(repoMock, "", ""));

        RevCommit revCommitMock = mock(RevCommit.class);
        RevWalk revWalkMock = new RevWalk(getRepository()) {

            @Override
            public void close() {}

            @Override
            public RevCommit parseCommit(AnyObjectId id)
                    throws MissingObjectException, IncorrectObjectTypeException, IOException {
                return revCommitMock;
            }
        };
        JGit git = new JGit(getBackgroundServiceMock()) {
            @Override
            RevWalk getRevWalk(Repository repo) {
                return revWalkMock;
            }

            @Override
            boolean checkDirCacheCheck(Repository repo, RevTree firstTree, RevTree secondTree)
                    throws NoWorkTreeException, CorruptObjectException, IOException {
                return true;
            }
        };
        Assert.assertTrue(git.isConflictsBetweenTwoBranches(repoMock, "", ""));

        git = new JGit(getBackgroundServiceMock()) {
            @Override
            RevWalk getRevWalk(Repository repo) {
                return revWalkMock;
            }

            @Override
            boolean checkDirCacheCheck(Repository repo, RevTree firstTree, RevTree secondTree)
                    throws NoWorkTreeException, CorruptObjectException, IOException {
                throw mock(IOException.class);
            }
        };
        Assert.assertTrue(git.isConflictsBetweenTwoBranches(repoMock, "", ""));
    }

    @Test
    public void isConflictsBetweenTwoBranchesCorrectDataTest() {
        Repository repoMock = getRepo(NAME_BRANCH);

        RevCommit revCommitMock = mock(RevCommit.class);
        RevWalk revWalkMock = new RevWalk(getRepository()) {
            @Override
            public void close() {}

            @Override
            public RevCommit parseCommit(AnyObjectId id)
                    throws MissingObjectException, IncorrectObjectTypeException, IOException {
                return revCommitMock;
            }
        };
        JGit git = new JGit(getBackgroundServiceMock()) {
            @Override
            RevWalk getRevWalk(Repository repo) {
                return revWalkMock;
            }

            @Override
            boolean checkDirCacheCheck(Repository repo, RevTree firstTree, RevTree secondTree)
                    throws NoWorkTreeException, CorruptObjectException, IOException {
                return false;
            }
        };
        Assert.assertFalse(git.isConflictsBetweenTwoBranches(repoMock, "", ""));
    }

    @Test
    public void addDeletedFileIncorrectParametersTest() throws NoFilepatternException, GitAPIException {
        Assert.assertFalse(getJGitMock(getGitMock()).addDeletedFile(null, getProject(true), true));
        Assert.assertFalse(getJGitMock(getGitMock()).addDeletedFile(fileName, getProject(false), true));
        Assert.assertFalse(getJGitMock(getGitMock()).addDeletedFile(fileName, null, false));
        Assert.assertFalse(getJGitMock(null).addDeletedFile(fileName, getProject(true), false));

        Git gitMock = getGitMock();
        RmCommand rmCommandMock = mock(RmCommand.class);
        Mockito.when(rmCommandMock.setCached(true)).thenReturn(rmCommandMock);
        Mockito.when(rmCommandMock.addFilepattern(fileName)).thenReturn(rmCommandMock);
        Mockito.when(rmCommandMock.call()).thenThrow(getGitAPIException());
        Mockito.when(gitMock.rm()).thenReturn(rmCommandMock);

        Assert.assertFalse(getJGitMock(gitMock).addDeletedFile(fileName, getProject(true), true));
    }

    @Test
    public void addDeletedFileCorrectTest() throws NoFilepatternException, GitAPIException {
        Git gitMock = getGitMock();
        RmCommand rmCommandMock = mock(RmCommand.class);

        Mockito.when(rmCommandMock.setCached(true)).thenReturn(rmCommandMock);
        Mockito.when(rmCommandMock.addFilepattern(fileName)).thenReturn(rmCommandMock);
        Mockito.when(rmCommandMock.call()).thenReturn(getDirCache());
        Mockito.when(gitMock.rm()).thenReturn(rmCommandMock);

        Assert.assertTrue(getJGitMock(gitMock).addDeletedFile(fileName, getProject(true), true));
    }

    @Test
    public void addDeletedFilesIncorrectParametersTest() throws NoFilepatternException, GitAPIException {
        List<String> files = new ArrayList<>(Arrays.asList("xyz", "abc"));
        Assert.assertTrue(getJGitMock(getGitMock()).addDeletedFiles(null, getProject(true), true).isEmpty());
        Assert.assertTrue(getJGitMock(getGitMock()).addDeletedFiles(new ArrayList<>(), getProject(true), true).isEmpty());
        Assert.assertTrue(getJGitMock(getGitMock()).addDeletedFiles(files, null, false).isEmpty());
        Assert.assertTrue(getJGitMock(getGitMock()).addDeletedFiles(files, getProject(false), false).isEmpty());
        Assert.assertTrue(getJGitMock(null).addDeletedFiles(files, getProject(true), false).isEmpty());

        Git gitMock = getGitMock();
        RmCommand rmCommandMock = mock(RmCommand.class);
        Mockito.when(rmCommandMock.setCached(true)).thenReturn(rmCommandMock);
        Mockito.when(rmCommandMock.addFilepattern(Mockito.anyString())).thenReturn(rmCommandMock);
        Mockito.when(rmCommandMock.call()).thenThrow(getGitAPIException());
        Mockito.when(gitMock.rm()).thenReturn(rmCommandMock);

        Assert.assertTrue(getJGitMock(gitMock).addDeletedFiles(files, getProject(true), true).isEmpty());
    }

    @Test
    public void addDeletedFilesCorrectTest() throws NoFilepatternException, GitAPIException {
        List<String> files = new ArrayList<>(Arrays.asList("xyz", "abc"));

        Git gitMock = getGitMock();
        RmCommand rmCommandMock = mock(RmCommand.class);
        Mockito.when(rmCommandMock.setCached(true)).thenReturn(rmCommandMock);
        Mockito.when(rmCommandMock.addFilepattern(Mockito.anyString())).thenReturn(rmCommandMock);
        Mockito.when(rmCommandMock.call()).thenReturn(getDirCache());
        Mockito.when(gitMock.rm()).thenReturn(rmCommandMock);

        Assert.assertFalse(getJGitMock(gitMock).addDeletedFiles(files, getProject(true), true).isEmpty());
    }

    @Test
    public void resetChangedFilesIncorrectParametersTest() throws CheckoutConflictException, GitAPIException {
        List<String> files = new ArrayList<>(Arrays.asList("xyz", "abc"));
        Assert.assertTrue(getJGitMock(getGitMock()).resetChangedFiles(null, getProject(true)).isEmpty());
        Assert.assertTrue(getJGitMock(getGitMock()).resetChangedFiles(new ArrayList<>(), getProject(true)).isEmpty());
        Assert.assertTrue(getJGitMock(getGitMock()).resetChangedFiles(files, null).isEmpty());
        Assert.assertTrue(getJGitMock(getGitMock()).resetChangedFiles(files, getProject(false)).isEmpty());
        Assert.assertTrue(getJGitMock(null).resetChangedFiles(files, getProject(true)).isEmpty());

        Git gitMock = getGitMock();
        ResetCommand resetCommandMock = mock(ResetCommand.class);
        Mockito.when(resetCommandMock.setRef(Constants.HEAD)).thenReturn(resetCommandMock);
        Mockito.when(resetCommandMock.addPath(Mockito.anyString())).thenReturn(resetCommandMock);
        Mockito.when(resetCommandMock.call()).thenThrow(getGitAPIException());
        Mockito.when(gitMock.reset()).thenReturn(resetCommandMock);
        Assert.assertTrue(getJGitMock(gitMock).resetChangedFiles(files, getProject(true)).isEmpty());
    }

    @Test
    public void resetChangedFilesCorrectTest() throws CheckoutConflictException, GitAPIException {
        List<String> files = new ArrayList<>(Arrays.asList("xyz", "abc"));
        Git gitMock = getGitMock();
        ResetCommand resetCommandMock = mock(ResetCommand.class);
        Mockito.when(resetCommandMock.setRef(Constants.HEAD)).thenReturn(resetCommandMock);
        Mockito.when(resetCommandMock.addPath(Mockito.anyString())).thenReturn(resetCommandMock);
        Mockito.when(gitMock.reset()).thenReturn(resetCommandMock);
        Mockito.when(resetCommandMock.call()).thenReturn(mock(Ref.class));
        Assert.assertFalse(getJGitMock(gitMock).resetChangedFiles(files, getProject(true)).isEmpty());
    }

    private final List<String> correctFiles = Arrays.asList("test", "564.txt");

    @Test
    public void replaceWithHEADRevisionWrongParameters() {
        Project clonedProject = getProject(true);

        Assert.assertFalse(getJGitMock(null).replaceFilesWithHEADRevision(null, correctFiles));
        Assert.assertFalse(getJGitMock(null).replaceFilesWithHEADRevision(new Project(), correctFiles));
        Assert.assertFalse(getJGitMock(null).replaceFilesWithHEADRevision(clonedProject, null));
        Assert.assertFalse(getJGitMock(null).replaceFilesWithHEADRevision(clonedProject, new ArrayList<>()));
    }

    @Test
    public void replaceWithHEADRevisionGitDoesntExist() {
        Project clonedProject = getProject(true);

        boolean isSuccessful = getJGitMock(null).replaceFilesWithHEADRevision(clonedProject, correctFiles);

        Assert.assertFalse(isSuccessful);
    }

    @Test
    public void replaceWithHEADRevisionFailedReplaced() throws GitAPIException {
        String correctFileName = "test";
        Project clonedProject = getProject(true);
        Git gitMock = getGitMock();
        CheckoutCommand checkCommandMock = mock(CheckoutCommand.class);
        // mock CheckoutCommand and Git methods
        Mockito.when(checkCommandMock.addPath(correctFileName)).thenReturn(checkCommandMock);
        Mockito.when(checkCommandMock.call()).thenThrow(getGitAPIException());
        Mockito.when(gitMock.checkout()).thenReturn(checkCommandMock);

        boolean isSuccessful = getJGitMock(gitMock).replaceFilesWithHEADRevision(clonedProject, correctFiles);

        Assert.assertFalse(isSuccessful);
    }

    @Test
    public void replaceWithHEADRevisionSuccessfullyReplaced() throws GitAPIException {
        String correctFileName = "test";
        Project clonedProject = getProject(true);
        Git gitMock = getGitMock();
        CheckoutCommand checkCommandMock = mock(CheckoutCommand.class);
        // mock CheckoutCommand and Git methods
        Mockito.when(checkCommandMock.addPath(correctFileName)).thenReturn(checkCommandMock);
        Mockito.when(checkCommandMock.call()).thenReturn(mock(Ref.class));
        Mockito.when(gitMock.checkout()).thenReturn(checkCommandMock);

        boolean isSuccessful = getJGitMock(gitMock).replaceFilesWithHEADRevision(clonedProject, correctFiles);

        Assert.assertTrue(isSuccessful);
    }


    /*************************************************************************************************/
    private Project getProject(boolean isCorrectProject) {
        if (!isCorrectProject) {
            return new Project();
        }
        Project projectCorrect = new Project() {
            @Override
            protected boolean checkPath(Path pathToProject) {
                return true;
            };
        };
        projectCorrect.setPathToClonedProject(".path");
        projectCorrect.setClonedStatus(true);
        return projectCorrect;
    }

    private List<Project> getProjects() {
        //Please use COUNT_INCORRECT_PROJECT if you add here new incorrect value
        List<Project> listProjects = new ArrayList<>();
        listProjects.add(getProject(true));
        listProjects.add(null);
        listProjects.add(getProject(false));
        listProjects.add(new Project());
        return listProjects;
    }

    private long getCountIncorrectProject(List<Project> projects) {
        return projects.stream()
                .filter((project) -> project == null || !project.isCloned())
                .count();
    }

    private long getCountCorrectProject(List<Project> projects) {
        return (projects.size() - getCountIncorrectProject(projects));
    }

    private long getCountCorrectStatuses(Map<Project, JGitStatus> statuses){
        return  statuses.entrySet().stream()
                .map(Map.Entry::getValue)
                .filter(status -> status.equals(JGitStatus.SUCCESSFUL))
                .count();
    }

    private long getCountIncorrectStatuses(Map<Project, JGitStatus> statuses){
        return  statuses.entrySet().stream()
                .map(Map.Entry::getValue)
                .filter(status -> status.equals(JGitStatus.FAILED))
                .count();
    }

    private JGit getJGitMock(Git gitMock) {
        if (gitMock == null) {
            return new JGit(getBackgroundServiceMock()) {
                @Override
                protected Git getGit(String path) throws IOException {
                    throw mock(IOException.class);
                }
            };
        }

        JGit correctJGitMock = new JGit(getBackgroundServiceMock()) {
            @Override
            protected User getUserData() {
                User user = new User("Lyudmila", "ld@email.com");
                return user;
            }

            @Override
            protected Git getGit(String path) throws IOException {
                return gitMock;
            }

            @Override
            protected BranchConfig getBranchConfig(Config config, String branchName) {

                return new BranchConfig(config, branchName) {
                    @Override
                    public String getTrackingBranch() {
                        return NAME_TRACKING_BRANCH;
                    }
                };
            }
        };
        return correctJGitMock;
    }

    private Git getGitMock() {
        return mock(Git.class);
    }

    private Repository getRepository() {
        return mock(Repository.class);
    }

    private Repository getRepo(String nameBranch) {
        BaseRepositoryBuilder<?, ?> buildMock = mock(BaseRepositoryBuilder.class);
        if (nameBranch == null) {
            return new Repository(buildMock) {

                @Override
                public void close() {
                    // Do nothing
                }

                @Override
                public Ref exactRef(String name) throws IOException {
                    return null;
                }

                @Override
                public String getFullBranch() throws IOException {
                    throw new IOException();
                }

                @Override
                public String getBranch() throws IOException {
                    throw new IOException();
                }

                @Override
                public void scanForRepoChanges() throws IOException {
                }

                @Override
                public void notifyIndexChanged() {
                }

                @Override
                public ReflogReader getReflogReader(String refName) throws IOException {
                    return null;
                }

                @Override
                public RefDatabase getRefDatabase() {
                    return null;
                }

                @Override
                public ObjectDatabase getObjectDatabase() {
                    return null;
                }

                @Override
                public StoredConfig getConfig() {
                    return mock(StoredConfig.class);
                }

                @Override
                public AttributesNodeProvider createAttributesNodeProvider() {
                    return null;
                }

                @Override
                public void create(boolean bare) throws IOException {

                }
            };
        }

        Ref refMock = mock(Ref.class);
        ObjectId objectIdMock = mock(ObjectId.class);
        Mockito.when(refMock.getObjectId()).thenReturn(objectIdMock);
        Repository repoMock = new Repository(buildMock) {

            @Override
            public void close() {
                // Do nothing
            }

            @Override
            public Ref exactRef(String name) throws IOException {
                return refMock;
            }

            @Override
            public String getFullBranch() throws IOException {
                return Constants.R_HEADS + nameBranch;
            }

            @Override
            public String getBranch() throws IOException {
                return nameBranch;
            }

            @Override
            public void scanForRepoChanges() throws IOException {
            }

            @Override
            public void notifyIndexChanged() {
            }

            @Override
            public ReflogReader getReflogReader(String refName) throws IOException {
                return null;
            }

            @Override
            public RefDatabase getRefDatabase() {
                return null;
            }

            @Override
            public ObjectDatabase getObjectDatabase() {
                return null;
            }

            @Override
            public StoredConfig getConfig() {
                return mock(StoredConfig.class);
            }

            @Override
            public AttributesNodeProvider createAttributesNodeProvider() {
                return null;
            }

            @Override
            public void create(boolean bare) throws IOException {

            }
        };
        return repoMock;
    }

    private ListBranchCommand getListCommandMock(Ref ref) {
        ListBranchCommand listCommandMock = new ListBranchCommand(getRepository()) {
            @Override
            public List<Ref> call() throws GitAPIException {
                List<Ref> refs = new ArrayList<>();
                refs.add(ref);
                return refs;
            }
        };
        return listCommandMock;
    }

    private GitAPIException getGitAPIException() {
        return mock(GitAPIException.class);
    }

    private DirCache getDirCache() {
        return mock(DirCache.class);
    }

    private BackgroundService getBackgroundServiceMock() {
        return mock(BackgroundServiceImpl.class);
    }

    class EmptyListener implements ProgressListener {

        @Override
        public void onSuccess(Object... t) {
        }

        @Override
        public void onError(Object... t) {
        }

        @Override
        public void onStart(Object... t) {
        }

        @Override
        public void onFinish(Object... t) {
        }
    }

    private List<Project> getProjects(int count) {
        List<Project> projects = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            projects.add(new Project());
        }
        return projects;
    }

    private List<Project> getCorrectProject(int countProject) {
        List<Project> projects = new ArrayList<>();
        for (int i = 0; i < countProject; i++) {
            Project pr = new Project();
            pr.setClonedStatus(true);
            pr.setPathToClonedProject(".");
            projects.add(pr);
        }
        return projects;
    }

    private OperationProgressListener getEmptyOperationProgressListener() {
        return new OperationProgressListener(
                Mockito.mock(ProgressDialog.class),
                ApplicationState.CLONE) {
            @Override
            public void onSuccess(Object... t) {}
            @Override
            public void onError(Object... t) {}
            @Override
            public void onStart(Object... t) {}
            @Override
            public void doOnFinishJob(Object... t) {}
        };
    }
}
