package com.lgc.gitlabtool.git.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.lgc.gitlabtool.git.connections.HttpResponseHolder;
import com.lgc.gitlabtool.git.connections.RESTConnector;
import com.lgc.gitlabtool.git.connections.token.CurrentUser;
import com.lgc.gitlabtool.git.entities.Group;
import com.lgc.gitlabtool.git.entities.Project;
import com.lgc.gitlabtool.git.entities.ProjectStatus;
import com.lgc.gitlabtool.git.jgit.JGit;
import com.lgc.gitlabtool.git.listeners.stateListeners.ApplicationState;
import com.lgc.gitlabtool.git.project.nature.projecttype.DSGProjectType;
import com.lgc.gitlabtool.git.project.nature.projecttype.UnknownProjectType;
import com.lgc.gitlabtool.git.util.PathUtilities;

public class ProjectServiceImplTest {

    private ProjectService _projectService;

    private ProjectTypeService _projectTypeService;
    private StateService _stateService;
    private RESTConnector _connector;
    private ConsoleService _consoleService;
    private GitService _gitService;
    private JSONParserService _jsonParserService;
    private CurrentUser _currentUser;
    private PathUtilities _pathUtilities;
    private JGit _jGit;

    @Before
    public void init() {
        _projectTypeService = mock(ProjectTypeService.class);
        _stateService = mock(StateService.class);
        _connector = mock(RESTConnector.class);
        _consoleService = mock(ConsoleService.class);
        _jsonParserService = mock(JSONParserService.class);
        _jGit = mock(JGit.class);
        _currentUser = mock(CurrentUser.class);
        _gitService = mock(GitService.class);
        _pathUtilities = mock(PathUtilities.class);

        _projectService = new ProjectServiceImpl(_connector, _projectTypeService, _stateService,
                _consoleService, _gitService, _jsonParserService, _currentUser, _pathUtilities, _jGit);
    }

    @After
    public void clear() {
        _projectTypeService = null;
        _stateService = null;
        _connector = null;
        _consoleService = null;
        _gitService = null;
        _jGit = null;
        _gitService = null;
        _projectService = null;
    }

    @Test
    public void getIdsProjectsWrongParameter() {
        assertTrue(_projectService.getIdsProjects(null).isEmpty());
        assertTrue(_projectService.getIdsProjects(new ArrayList<>()).isEmpty());
    }

    @Test
    public void getIdsProjectsSuccessfully() {
        List<Project> projects = new ArrayList<>();
        int firstId = 25;
        int secondId = 305;
        projects.add(getProjectWithId(firstId));
        projects.add(getProjectWithId(secondId));

        List<Integer> result = _projectService.getIdsProjects(projects);

        assertFalse(result.isEmpty());
        assertEquals(result.size(), projects.size());
        assertTrue(result.containsAll(Arrays.asList(firstId, secondId)));
    }

    @Test
    public void getCorrectProjectsWrongParameter() {
        assertTrue(_projectService.getCorrectProjects(null).isEmpty());
        assertTrue(_projectService.getCorrectProjects(new ArrayList<>()).isEmpty());
    }

    @Test
    public void getCorrectProjectsSuccessfully() {
        List<Project> projects = new ArrayList<>();
        projects.add(new Project());
        projects.add(getClonedProject());

        List<Project> result = _projectService.getCorrectProjects(projects);

        assertFalse(result.isEmpty());
        assertNotEquals(result.size(), projects.size());
    }

    @Test
    public void projectIsClonedAndWithoutConflictsWrongParameter() {
        assertFalse(_projectService.projectIsClonedAndWithoutConflicts(null));
        assertFalse(_projectService.projectIsClonedAndWithoutConflicts(new Project()));
    }

    @Test
    public void projectIsClonedAndWithoutConflictsWrongFailed() {
        Project project = getProjectWithConflicts();

        boolean isCorrectProjects = _projectService.projectIsClonedAndWithoutConflicts(project);

        assertFalse(isCorrectProjects);
    }

    @Test
    public void projectIsClonedAndWithoutConflictsSuccessfully() {
        Project project = getClonedProject();

        boolean isCorrectProjects = _projectService.projectIsClonedAndWithoutConflicts(project);

        assertTrue(isCorrectProjects);
    }

    @Test(expected=IllegalArgumentException.class)
    public void getProjectsNullGroup() {
        _projectService.getProjects(null);
    }

    @Test
    public void getProjectsNullTokenValue() {
        setTokenValue(false);

        Collection<Project> result = _projectService.getProjects(new Group());

        assertTrue(result.isEmpty());
    }

    @Test
    public void getProjectsErrorFromGitLab() {
        setTokenValue(true);
        HttpResponseHolder httpResponseHolderMock = getHttpResponseHolder(false, "1", "test_json");
        when(_connector.sendGet(anyString(), eq(null), anyMap())).thenReturn(httpResponseHolderMock);

        Collection<Project> result = _projectService.getProjects(getGroupWithSubGroup(5));

        assertEquals(result, null);
    }

    @Test
    public void getProjectsOnlyOnePage() {
        int countSubGroup = 5;
        Group testedGroup = getGroupWithSubGroup(countSubGroup);
        // mock of answers from the GitLab
        setTokenValue(true);
        HttpResponseHolder httpResponseHolderMock = getHttpResponseHolder(true, "1", "test_json");
        when(_connector.sendGet(anyString(), eq(null), anyMap())).thenReturn(httpResponseHolderMock);
        // mock of json parser
        Collection<Object> groupProjects = Arrays.asList(new Project(), new Project()); //for each group (include subgroups)
        when(_jsonParserService.parseToCollectionObjects(anyString(), Mockito.any(Type.class))).thenReturn(groupProjects);

        Collection<Project> result = _projectService.getProjects(testedGroup);

        assertEquals(groupProjects.size() * (countSubGroup + 1), result.size());
    }

    @Test
    public void getProjectsFewPages() {
        int countSubGroup = 3;
        Group testedGroup = getGroupWithSubGroup(countSubGroup);
        // mock of answers from the GitLab
        setTokenValue(true);
        int countProjectPages = 3;
        HttpResponseHolder httpResponseHolderMock = getHttpResponseHolder(true, "" + countProjectPages, "test_json");
        when(_connector.sendGet(anyString(), eq(null), anyMap())).thenReturn(httpResponseHolderMock);
        // mock of json parser
        Collection<Object> groupProjects = new ArrayList<>(Arrays.asList(new Project())); //for each group (include subgroups)
        when(_jsonParserService.parseToCollectionObjects(anyString(), Mockito.any(Type.class))).thenReturn(groupProjects);

        Collection<Project> result = _projectService.getProjects(testedGroup);

        int countGroups = countSubGroup + 1;
        int countGroupProjects = groupProjects.size() * countProjectPages;
        assertEquals(countGroupProjects * countGroups, result.size());
    }

    @Test(expected=IllegalArgumentException.class)
    public void loadProjectsNullGroup() {
        _projectService.loadProjects(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void loadProjectsGroupIsNotCloned() {
        _projectService.loadProjects(new Group());
    }

    @Test
    public void loadProjectsErrorFromGitLab() {
        _projectService = getProjectServiceGetProjectMock(null);
        Collection<Project> result = _projectService.loadProjects(getClonedGroup());

        assertEquals(result, null);
    }

    @Test
    public void loadProjectsGroupDoesNotHaveProjects() {
        _projectService = getProjectServiceGetProjectMock(Collections.emptyList());

        Collection<Project> result = _projectService.loadProjects(getClonedGroup());

        assertTrue(result.isEmpty());
    }

    @Test
    public void loadProjectsGroupDoesNotHaveClonedProjects() {
        when(_pathUtilities.getFolders(Mockito.any(Path.class))).thenReturn(new ArrayList<>());
        List<Project> projectsFromGitLab = new ArrayList<>(Arrays.asList(new Project(), new Project()));
        _projectService = getProjectServiceGetProjectMock(projectsFromGitLab);

        Collection<Project> result = _projectService.loadProjects(getClonedGroup());

        assertFalse(result.isEmpty());
        assertEquals(projectsFromGitLab.size(), result.size());

        // check that project statuses weren't updated
        Project resultProject = result.iterator().next();
        assertFalse(resultProject.isCloned());
        assertTrue(resultProject.getProjectStatus().getCurrentBranch().isEmpty());
        assertEquals(resultProject.getPath(), null);
        assertEquals(resultProject.getProjectType(), null);
    }

    @Test
    public void loadProjectsGroupHasClonedProjects() {
        List<Project> projectsFromGitLab = getNotClonedProjects();
        List<String> clonedProjects = new ArrayList<>(Arrays.asList("test project"));
        when(_pathUtilities.getFolders(any(Path.class))).thenReturn(clonedProjects);
        when(_pathUtilities.isExistsAndDirectory(any(Path.class))).thenReturn(true);
        when(_projectTypeService.getProjectType(any(Project.class))).thenReturn(new DSGProjectType());
        gitServiceGetProjectStatusMock();
        _projectService = getProjectServiceGetProjectMock(projectsFromGitLab);

        Collection<Project> result = _projectService.loadProjects(getClonedGroup());

        assertFalse(result.isEmpty());
        assertEquals(projectsFromGitLab.size(), result.size());

        // check that project statuses were updated
        Project resultProject = result.iterator().next();
        assertTrue(resultProject.isCloned());
        assertTrue(resultProject.getProjectStatus().hasChanges());
        assertTrue(resultProject.getProjectType() instanceof DSGProjectType);
        assertFalse(resultProject.getProjectStatus().getCurrentBranch().isEmpty());
    }

    @Test
    public void updateProjectTypeAndStatusNullProject() {
        boolean result = _projectService.updateProjectTypeAndStatus(null);

        assertFalse(result);
    }

    @Test
    public void updateProjectStatusNullProject() {
        boolean result = _projectService.updateProjectStatus(null);

        assertFalse(result);
    }

    @Test
    public void updateProjectStatusProjectIsNotCloned() {
        boolean result = _projectService.updateProjectStatus(new Project());

        assertFalse(result);
    }

    @Test
    public void updateProjectStatusesNullList() {
        boolean result = _projectService.updateProjectStatuses(null);

        assertFalse(result);
    }

    @Test
    public void updateProjectStatusesEmptyList() {
        boolean result = _projectService.updateProjectStatuses(new ArrayList<>());

        assertFalse(result);
    }

    @Test
    public void updateProjectStatusesSuccess() {
        List<Project> projects = Arrays.asList(getClonedProject(), getClonedProject(), null);
        gitServiceGetProjectStatusMock();

        boolean result = _projectService.updateProjectStatuses(projects);

        assertTrue(result);
        assertTrue(projects.get(0).getProjectStatus().hasChanges());
        assertFalse(projects.get(0).getProjectStatus().getCurrentBranch().isEmpty());
    }

    @Test(expected=IllegalArgumentException.class)
    public void createProjectNullGroup() {
        _projectService.createProject(null, "name", new UnknownProjectType(), null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void createProjectNotClonedGroup() {
        _projectService.createProject(new Group(), "name", new UnknownProjectType(), null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void createProjectNullName() {
        _projectService.createProject(getClonedGroup(), null, new UnknownProjectType(), null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void createProjectEmptyName() {
        _projectService.createProject(getClonedGroup(), "", new UnknownProjectType(), null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void createProjectNullProjectType() {
        _projectService.createProject(getClonedGroup(), "name", null, null);
    }

    @Test
    public void createProjectErrorFromGitLab() {
        CreateProjectTestListener progressListener = new CreateProjectTestListener();
        _projectService = getProjectServiceGetProjectMock(null);

        _projectService.createProject(getClonedGroup(), "name", new UnknownProjectType(), progressListener);

        assertFalse(progressListener.isSuccessfulOperation());
    }

    @Test
    public void createProjectProjectExist() {
        CreateProjectTestListener progressListener = new CreateProjectTestListener();
        _projectService = getProjectServiceGetProjectMock(Arrays.asList(new Project(), getProjectWithName("test name")));

        _projectService.createProject(getClonedGroup(), "test name", new UnknownProjectType(), progressListener);

        assertFalse(progressListener.isSuccessfulOperation());
    }

    @Test
    public void createProjectInvalidToken() {
        setTokenValue(false);
        CreateProjectTestListener progressListener = new CreateProjectTestListener();
        _projectService = getProjectServiceGetProjectMock(Arrays.asList(new Project(), getProjectWithName("test name")));

        _projectService.createProject(getClonedGroup(), "new test name", new UnknownProjectType(), progressListener);

        assertFalse(progressListener.isSuccessfulOperation());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void createProjectErrorCreatingRemote() {
        setTokenValue(true);
        setSendPostMock(false, "1", null);
        CreateProjectTestListener progressListener = new CreateProjectTestListener();
        when(_jsonParserService.parseToObject(eq(null), any(Class.class))).thenReturn(null);
        _projectService = getProjectServiceGetProjectMock(Arrays.asList(new Project(), getProjectWithName("test name")));

        _projectService.createProject(getClonedGroup(), "new test name", new UnknownProjectType(), progressListener);

        assertFalse(progressListener.isSuccessfulOperation());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void createProjectErrorCreatingLocal() {
        setTokenValue(true);
        setSendPostMock(true, "1", "test_json");
        CreateProjectTestListener progressListener = new CreateProjectTestListener();
        when(_jsonParserService.parseToObject(anyString(), any(Class.class))).thenReturn(getProjectWithName("new test name"));
        when(_pathUtilities.isExistsAndDirectory(any(Path.class))).thenReturn(false);
        _projectService = getProjectServiceGetProjectMock(Arrays.asList(new Project(), getProjectWithName("test name")));

        _projectService.createProject(getClonedGroup(), "new test name", new UnknownProjectType(), progressListener);

        assertFalse(progressListener.isSuccessfulOperation());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void createProjectCloneToLocalFailed() {
        setTokenValue(true);
        jGitCloneMock(false);
        setSendPostMock(true, "1", "test_json");
        CreateProjectTestListener progressListener = new CreateProjectTestListener();
        when(_jsonParserService.parseToObject(anyString(), any(Class.class))).thenReturn(getProjectWithName("new test name"));
        when(_pathUtilities.isExistsAndDirectory(any(Path.class))).thenReturn(true);
        _projectService = getProjectServiceGetProjectMock(Arrays.asList(new Project(), getProjectWithName("test name")));

        _projectService.createProject(getClonedGroup(), "new test name", new UnknownProjectType(), progressListener);

        assertFalse(progressListener.isSuccessfulOperation());
    }


    @SuppressWarnings("unchecked")
    @Test
    public void createProjectCreateWithoutStructuresType() {
        setTokenValue(true);
        jGitCloneMock(true);
        setSendPostMock(true, "1", "test_json");
        CreateProjectTestListener progressListener = new CreateProjectTestListener();
        when(_jsonParserService.parseToObject(anyString(), any(Class.class))).thenReturn(getProjectWithName("new test name"));
        when(_pathUtilities.isExistsAndDirectory(any(Path.class))).thenReturn(true);
        when(_jGit.commitAndPush(anyList(), anyString(), eq(null), eq(null), eq(null), eq(null), eq(EmptyProgressListener.get()))).thenReturn(null);
        _projectService = getProjectServiceGetProjectMock(Arrays.asList(new Project(), getProjectWithName("test name")));

        _projectService.createProject(getClonedGroup(), "new test name", new UnknownProjectType(), progressListener);

        assertTrue(progressListener.isSuccessfulOperation());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void createProjectCreateStructuresTypeSuccess() {
        setTokenValue(true);
        jGitCloneMock(true);
        setSendPostMock(true, "1", "test_json");
        CreateProjectTestListener progressListener = new CreateProjectTestListener();
        when(_jsonParserService.parseToObject(anyString(), any(Class.class))).thenReturn(getProjectWithName("new test name"));
        when(_pathUtilities.isExistsAndDirectory(any(Path.class))).thenReturn(true);
        when(_pathUtilities.createPath(any(Path.class), eq(false))).thenReturn(true);
        when(_jGit.addUntrackedFilesToIndex(Mockito.anyCollection(), any(Project.class))).thenReturn(null);
        when(_jGit.commitAndPush(anyList(), anyString(), eq(null), eq(null), eq(null), eq(null), eq(EmptyProgressListener.get()))).thenReturn(null);
        _projectService = getProjectServiceGetProjectMock(Arrays.asList(new Project(), getProjectWithName("test name")));

        _projectService.createProject(getClonedGroup(), "new test name", new DSGProjectType(), progressListener);

        assertTrue(progressListener.isSuccessfulOperation());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void createProjectCreateStructuresTypeFailed() {
        setTokenValue(true);
        jGitCloneMock(true);
        setSendPostMock(true, "1", "test_json");
        CreateProjectTestListener progressListener = new CreateProjectTestListener();
        Project project = getProjectWithName("new test name");
        project.setPath(".");
        when(_jsonParserService.parseToObject(anyString(), any(Class.class))).thenReturn(project);
        when(_pathUtilities.isExistsAndDirectory(any(Path.class))).thenReturn(true);
        when(_pathUtilities.createPath(any(Path.class), eq(false))).thenReturn(false);
        when(_pathUtilities.deletePath(any(Path.class))).thenReturn(true);
        when(_jGit.addUntrackedFilesToIndex(Mockito.anyCollection(), any(Project.class))).thenReturn(null);
        when(_jGit.commitAndPush(anyList(), anyString(), eq(null), eq(null), eq(null), eq(null), eq(EmptyProgressListener.get()))).thenReturn(null);
        _projectService = getProjectServiceGetProjectMock(Arrays.asList(new Project(), getProjectWithName("test name")));

        _projectService.createProject(getClonedGroup(), "new test name", new DSGProjectType(), progressListener);

        assertTrue(progressListener.isSuccessfulOperation());
    }

    @Test
    public void hasShadowNullList() {
        boolean result = _projectService.hasShadow(null);

        assertFalse(result);
    }

    @Test
    public void hasShadowEmptyList() {
        boolean result = _projectService.hasShadow(new ArrayList<>());

        assertFalse(result);
    }

    @Test
    public void hasShadowSuccess() {
        List<Project> projects = new ArrayList<>();
        projects.add(getClonedProject());
        projects.add(null);
        projects.add(new Project());

        boolean result = _projectService.hasShadow(projects);

        assertTrue(result);
    }


    @Test
    public void hasClonedNullList() {
        boolean result = _projectService.hasCloned(null);

        assertFalse(result);
    }

    @Test
    public void hasClonedEmptyList() {
        boolean result = _projectService.hasCloned(new ArrayList<>());

        assertFalse(result);
    }

    @Test
    public void hasClonedSuccess() {
        List<Project> projects = new ArrayList<>();
        projects.add(getClonedProject());
        projects.add(null);
        projects.add(new Project());

        boolean result = _projectService.hasCloned(projects);

        assertTrue(result);
    }

    @Test(expected=IllegalArgumentException.class)
    public void cloneNullProject() {
        _projectService.clone(null, "path", EmptyProgressListener.get());
    }

    @Test(expected=IllegalArgumentException.class)
    public void cloneNullPath() {
        _projectService.clone(new ArrayList<>(), null, EmptyProgressListener.get());
    }

    @Test(expected=IllegalArgumentException.class)
    public void cloneNullProgressListener() {
        _projectService.clone(new ArrayList<>(), "path", null);
    }

    @Test
    public void cloneFailed() {
        CloneTestListener progressListener = new CloneTestListener();
        Mockito.doNothing().when(_stateService).stateON(ApplicationState.CLONE);
        when(_pathUtilities.isExistsAndDirectory(any(Path.class))).thenReturn(false);

        _projectService.clone(Arrays.asList(new Project()), ".", progressListener);

        assertFalse(progressListener.isSuccessfulOperation());
    }

    @Test
    public void cloneSuccess() {
        CloneTestListener progressListener = new CloneTestListener();
        Mockito.doNothing().when(_stateService).stateON(ApplicationState.CLONE);
        when(_pathUtilities.isExistsAndDirectory(any(Path.class))).thenReturn(true);
        jGitCloneMock(true);

        _projectService.clone(Arrays.asList(new Project()), ".", progressListener);

        assertTrue(progressListener.isSuccessfulOperation());
    }

    /*********************************************************************************************/

    private void jGitCloneMock(boolean isSuccess) {
        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ProgressListener progressListener = (ProgressListener) invocation.getArguments()[2];
                if (isSuccess) {
                    progressListener.onSuccess();
                } else {
                    progressListener.onError();
                    progressListener.onFinish();
                }
                return null;
            }
        }).when(_jGit).clone(anyList(), anyString(), any(ProgressListener.class));
    }

    private void gitServiceGetProjectStatusMock() {
        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Project project = (Project) invocation.getArguments()[0];
                project.setProjectStatus(getProjectStatus(true, false));
                return null;
            }
        }).when(_gitService).getProjectStatus(any());
    }

    private ProjectService getProjectServiceGetProjectMock(List<Project> projects) {
        return new ProjectServiceImpl(_connector, _projectTypeService, _stateService,
                _consoleService, _gitService, _jsonParserService, _currentUser, _pathUtilities, _jGit) {
            @Override
            public Collection<Project> getProjects(Group group) {
                return projects;
            }
        };
    }

    private void setSendPostMock(boolean isSuccess, String countPages, String body) {
        HttpResponseHolder httpResponseHolderMock = getHttpResponseHolder(isSuccess, countPages, body);
        when(_connector.sendPost(anyString(), anyMap(), anyMap())).thenReturn(httpResponseHolderMock);
    }

    private void setTokenValue(boolean isValid) {
        when(_currentUser.getOAuth2TokenValue()).thenReturn(isValid ? "testTokenValue" : null);
        when(_currentUser.getPrivateTokenKey()).thenReturn(isValid ?  "testTokenKey" : null);
    }

    private Group getClonedGroup() {
        Group group = new Group();
        group.setClonedStatus(true);
        group.setPath(".");
        return group;
    }

    private Group getGroupWithSubGroup(int countSubGroup) {
        Group mainGroup = new Group();
        for (int i = 0; i < countSubGroup-1; i++) {
            Group subGroup = new Group();
            if (i == 0) {
                subGroup.addSubGroup(new Group());
            }
            mainGroup.addSubGroup(subGroup);
        }
        return mainGroup;
    }

    private Project getClonedProject() {
        Project project = new Project();
        project.setClonedStatus(true);
        project.setPath(".");
        ProjectStatus projectStatus = new ProjectStatus(false);
        project.setProjectStatus(projectStatus);
        return project;
    }

    private List<Project> getNotClonedProjects() {
        List<Project> projects = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Project project = new Project() {
                @Override
                protected boolean checkPath(Path pathToProject) {
                    return true;
                }
            };
            projects.add(project);
        }
        return projects;
    }

    private Project getProjectWithConflicts() {
        Project project = new Project();
        project.setClonedStatus(true);
        ProjectStatus projectStatus = getProjectStatus(false, true);
        project.setProjectStatus(projectStatus);
        return project;
    }


    private Project getProjectWithName(String name) {
        Project project = mock(Project.class);
        try {
            Class<?> gotClass = Class.forName(Project.class.getName());
            Object instance = gotClass.newInstance();
            setProjectFieldValue(instance, "_name", name);
            project = (Project) instance;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            System.err.println("Failed setting private id field in Project.class: " + e.getMessage());
        }
        return project;
    }

    private Project getProjectWithId(int id) {
        Project project = mock(Project.class);
        try {
            Class<?> gotClass = Class.forName(Project.class.getName());
            Object instance = gotClass.newInstance();
            setProjectFieldValue(instance, "_id", id);
            project = (Project) instance;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            System.err.println("Failed setting private id field in Project.class: " + e.getMessage());
        }
        return project;
    }

    private void setProjectFieldValue(Object instance, String fieldName, Object fieldValue) {
        try {
            Field foundField = instance.getClass().getDeclaredField(fieldName);
            foundField.setAccessible(true);
            foundField.set(instance, fieldValue);
            foundField.setAccessible(false);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            System.err.println("Failed setting private id field in Project.class: " + e.getMessage());
        }
    }

    private ProjectStatus getProjectStatus(boolean hasChanges, boolean hasConflicts) {
        return new ProjectStatus(hasChanges, 0, 0, "test_branch", null,
                hasConflicts ? new HashSet<>(Arrays.asList("conflict file")) : new HashSet<>(),
                new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
    }

    private HttpResponseHolder getHttpResponseHolder(boolean isSuccess, String countPages, String body) {
        HttpResponseHolder httpResponseHolderMock = mock(HttpResponseHolder.class);
        when(httpResponseHolderMock.getResponseCode()).thenReturn(isSuccess ? 200 : 101);
        when(httpResponseHolderMock.getBody()).thenReturn(body);
        Map<String, List<String>> prefereces = new HashMap<>();
        prefereces.put("X-Total-Pages", Arrays.asList(countPages));
        when(httpResponseHolderMock.getHeaderLines()).thenReturn(prefereces);
        return httpResponseHolderMock;
    }


    private class CreateProjectTestListener implements ProgressListener {

        private boolean _isSuccessfulOperation;

        @Override
        public void onSuccess(Object... t) {
            _isSuccessfulOperation = true;
        }

        @Override
        public void onError(Object... t) {
            _isSuccessfulOperation = false;
        }

        @Override
        public void onStart(Object... t) {

        }

        @Override
        public void onFinish(Object... t) {

        }

        public boolean isSuccessfulOperation() {
            return _isSuccessfulOperation;
        }
    }


    private class CloneTestListener implements ProgressListener {

        private boolean _isSuccessfulOperation;

        @Override
        public void onSuccess(Object... t) {
            _isSuccessfulOperation = true;
        }

        @Override
        public void onError(Object... t) {
            _isSuccessfulOperation = false;
        }

        @Override
        public void onStart(Object... t) {

        }

        @Override
        public void onFinish(Object... t) {

        }

        public boolean isSuccessfulOperation() {
            return _isSuccessfulOperation;
        }
    }
}
