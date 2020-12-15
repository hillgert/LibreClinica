/*
 * LibreClinica is distributed under the
 * GNU Lesser General Public License (GNU LGPL).

 * For details see: https://libreclinica.org/license
 * LibreClinica, copyright (C) 2020
 */
package org.akaza.openclinica.domain.user;
// default package
// Generated Jul 31, 2013 2:03:33 PM by Hibernate Tools 3.4.0.CR1

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.akaza.openclinica.domain.DataMapDomainObject;
import org.akaza.openclinica.domain.Status;
import org.akaza.openclinica.domain.datamap.CrfBean;
import org.akaza.openclinica.domain.datamap.CrfVersion;
import org.akaza.openclinica.domain.datamap.DiscrepancyNote;
import org.akaza.openclinica.domain.datamap.EventCrf;
import org.akaza.openclinica.domain.datamap.EventDefinitionCrf;
import org.akaza.openclinica.domain.datamap.Item;
import org.akaza.openclinica.domain.datamap.ItemData;
import org.akaza.openclinica.domain.datamap.ItemGroup;
import org.akaza.openclinica.domain.datamap.Section;
import org.akaza.openclinica.domain.datamap.Study;
import org.akaza.openclinica.domain.datamap.StudyEvent;
import org.akaza.openclinica.domain.datamap.StudyEventDefinition;
import org.akaza.openclinica.domain.datamap.StudyGroupClass;
import org.akaza.openclinica.domain.datamap.StudySubject;
import org.akaza.openclinica.domain.datamap.Subject;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;


/**
 * UserAccount generated by hbm2java
 */
@Entity
@Table(name = "user_account", schema = "public")
@GenericGenerator(name = "id-generator", strategy = "native", parameters = { @Parameter(name = "sequence_name", value = "user_account_user_id_seq") })

public class UserAccount extends DataMapDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int userId;
	private UserAccount userAccount;
	private UserType userType;
	private Status status;
	private String userName;
	private String passwd;
	private String firstName;
	private String lastName;
	private String email;
	private Study activeStudy;
	private String institutionalAffiliation;
	private Date dateCreated;
	private Date dateUpdated;
	private Date dateLastvisit;
	private Date passwdTimestamp;
	private String passwdChallengeQuestion;
	private String passwdChallengeAnswer;
	private String phone;
	private Integer updateId;
	private boolean enabled;
	private boolean accountNonLocked;
	private int lockCounter;
	private boolean runWebservices;
	private String accessCode;
	private String timeZone;
	private boolean enableApiKey;
	private String apiKey;
	
	private List<Item> items;
	private List<Section> sections ;
	private List<ItemGroup> itemGroups;
	private List<CrfBean> crfs;
	private List<UserAccount> userAccounts;
	private List<DiscrepancyNote> discrepancyNotesForAssignedUserId;
	private List<StudySubject> studySubjects;
	private List<EventDefinitionCrf> eventDefinitionCrfs;
	private List<StudyGroupClass> studyGroupClasses;
	private List<StudyEventDefinition> studyEventDefinitions ;
	private List<Subject> subjects;
	private List<DiscrepancyNote> discrepancyNotesForOwnerId;
	private List<ItemData> itemDatas ;
	private List<Study> studies ;
	private List<EventCrf> eventCrfs ;
	private List<StudyEvent> studyEvents ;
	private List<CrfVersion> crfVersions;

	public UserAccount() {
	}

	public UserAccount(int userId, boolean enabled, boolean accountNonLocked,
			int lockCounter, boolean runWebservices) {
		this.userId = userId;
		this.enabled = enabled;
		this.accountNonLocked = accountNonLocked;
		this.lockCounter = lockCounter;
		this.runWebservices = runWebservices;
	}

	@Id
	@Column(name = "user_id", unique = true, nullable = false)
    @GeneratedValue(generator = "id-generator")
	public int getUserId() {
		return this.userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id")
	public UserAccount getUserAccount() {
		return this.userAccount;
	}

	public void setUserAccount(UserAccount userAccount) {
		this.userAccount = userAccount;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_type_id")
	public UserType getUserType() {
		return this.userType;
	}

	public void setUserType(UserType userType) {
		this.userType = userType;
	}

	 @Type(type = "status")
	  @Column(name = "status_id")
	public Status getStatus() {
		return this.status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	@Column(name = "user_name", length = 64)
	public String getUserName() {
		return this.userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	@Column(name = "passwd")
	public String getPasswd() {
		return this.passwd;
	}

	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}

	@Column(name = "first_name", length = 50)
	public String getFirstName() {
		return this.firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	@Column(name = "last_name", length = 50)
	public String getLastName() {
		return this.lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@Column(name = "email", length = 120)
	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "active_study")
	public Study getActiveStudy() {
		return this.activeStudy;
	}

	public void setActiveStudy(Study activeStudy) {
		this.activeStudy = activeStudy;
	}

	@Column(name = "institutional_affiliation")
	public String getInstitutionalAffiliation() {
		return this.institutionalAffiliation;
	}

	public void setInstitutionalAffiliation(String institutionalAffiliation) {
		this.institutionalAffiliation = institutionalAffiliation;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "date_created", length = 4)
	public Date getDateCreated() {
		return this.dateCreated;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "date_updated", length = 4)
	public Date getDateUpdated() {
		return this.dateUpdated;
	}

	public void setDateUpdated(Date dateUpdated) {
		this.dateUpdated = dateUpdated;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "date_lastvisit", length = 8)
	public Date getDateLastvisit() {
		return this.dateLastvisit;
	}

	public void setDateLastvisit(Date dateLastvisit) {
		this.dateLastvisit = dateLastvisit;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "passwd_timestamp", length = 4)
	public Date getPasswdTimestamp() {
		return this.passwdTimestamp;
	}

	public void setPasswdTimestamp(Date passwdTimestamp) {
		this.passwdTimestamp = passwdTimestamp;
	}

	@Column(name = "passwd_challenge_question", length = 64)
	public String getPasswdChallengeQuestion() {
		return this.passwdChallengeQuestion;
	}

	public void setPasswdChallengeQuestion(String passwdChallengeQuestion) {
		this.passwdChallengeQuestion = passwdChallengeQuestion;
	}

	@Column(name = "passwd_challenge_answer")
	public String getPasswdChallengeAnswer() {
		return this.passwdChallengeAnswer;
	}

	public void setPasswdChallengeAnswer(String passwdChallengeAnswer) {
		this.passwdChallengeAnswer = passwdChallengeAnswer;
	}

	@Column(name = "phone", length = 64)
	public String getPhone() {
		return this.phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	@Column(name = "update_id")
	public Integer getUpdateId() {
		return this.updateId;
	}

	public void setUpdateId(Integer updateId) {
		this.updateId = updateId;
	}

	@Column(name = "enabled", nullable = false)
	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Column(name = "account_non_locked", nullable = false)
	public boolean isAccountNonLocked() {
		return this.accountNonLocked;
	}

	public void setAccountNonLocked(boolean accountNonLocked) {
		this.accountNonLocked = accountNonLocked;
	}

	@Column(name = "lock_counter", nullable = false)
	public int getLockCounter() {
		return this.lockCounter;
	}

	public void setLockCounter(int lockCounter) {
		this.lockCounter = lockCounter;
	}

	@Column(name = "run_webservices", nullable = false)
	public boolean isRunWebservices() {
		return this.runWebservices;
	}

	public void setRunWebservices(boolean runWebservices) {
		this.runWebservices = runWebservices;
	}

	
	
	/*@OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccount")
	public List getUserRoleAccesses() {
		return this.userRoleAccesses;
	}

	public void setUserRoleAccesses(List userRoleAccesses) {
		this.userRoleAccesses = userRoleAccesses;
	}*/

    @Column(name = "access_code", length = 64)
	public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    @Column(name = "time_zone", length = 255)
    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    @Column(name = "enable_api_key")
    public boolean isEnableApiKey() {
        return enableApiKey;
    }

    public void setEnableApiKey(boolean enableApiKey) {
        this.enableApiKey = enableApiKey;
    }

    @Column(name = "api_key", length = 255)
    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccount")
	public List<Item> getItems() {
		return this.items;
	}

	public void setItems(List<Item> items) {
		this.items = items;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccount")
	public List<Section> getSections() {
		return this.sections;
	}

	public void setSections(List<Section> sections) {
		this.sections = sections;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccount")
	public List<ItemGroup> getItemGroups() {
		return this.itemGroups;
	}

	public void setItemGroups(List<ItemGroup> itemGroups) {
		this.itemGroups = itemGroups;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccount")
	public List<CrfBean> getCrfs() {
		return this.crfs;
	}

	public void setCrfs(List<CrfBean> crfs) {
		this.crfs = crfs;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccount")
	public List<UserAccount> getUserAccounts() {
		return this.userAccounts;
	}

	public void setUserAccounts(List<UserAccount> userAccounts) {
		this.userAccounts = userAccounts;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccount")
	public List<DiscrepancyNote> getDiscrepancyNotesForAssignedUserId() {
		return this.discrepancyNotesForAssignedUserId;
	}

	public void setDiscrepancyNotesForAssignedUserId(
			List<DiscrepancyNote> discrepancyNotesForAssignedUserId) {
		this.discrepancyNotesForAssignedUserId = discrepancyNotesForAssignedUserId;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccount")
	public List<StudySubject> getStudySubjects() {
		return this.studySubjects;
	}

	public void setStudySubjects(List<StudySubject> studySubjects) {
		this.studySubjects = studySubjects;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccount")
	public List<EventDefinitionCrf> getEventDefinitionCrfs() {
		return this.eventDefinitionCrfs;
	}

	public void setEventDefinitionCrfs(List<EventDefinitionCrf> eventDefinitionCrfs) {
		this.eventDefinitionCrfs = eventDefinitionCrfs;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccount")
	public List<StudyGroupClass> getStudyGroupClasses() {
		return this.studyGroupClasses;
	}

	public void setStudyGroupClasses(List<StudyGroupClass> studyGroupClasses) {
		this.studyGroupClasses = studyGroupClasses;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccount")
	public List<StudyEventDefinition> getStudyEventDefinitions() {
		return this.studyEventDefinitions;
	}

	public void setStudyEventDefinitions(List<StudyEventDefinition> studyEventDefinitions) {
		this.studyEventDefinitions = studyEventDefinitions;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccount")
	public List<Subject> getSubjects() {
		return this.subjects;
	}

	public void setSubjects(List<Subject> subjects) {
		this.subjects = subjects;
	}

	/*@OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccount")
	public List<SubjectGroupMap> getSubjectGroupMaps() {
		return this.subjectGroupMaps;
	}

	public void setSubjectGroupMaps(List<SubjectGroupMap> subjectGroupMaps) {
		this.subjectGroupMaps = subjectGroupMaps;
	}*/

	/*@OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccount")
	public List getAuditUserLogins() {
		return this.auditUserLogins;
	}

	public void setAuditUserLogins(List auditUserLogins) {
		this.auditUserLogins = auditUserLogins;
	}*/

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccountByOwnerId")
	public List<DiscrepancyNote> getDiscrepancyNotesForOwnerId() {
		return this.discrepancyNotesForOwnerId;
	}

	public void setDiscrepancyNotesForOwnerId(List<DiscrepancyNote> discrepancyNotesForOwnerId) {
		this.discrepancyNotesForOwnerId = discrepancyNotesForOwnerId;
	}

	/*@OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccount")
	public List<StudyUserRole> getStudyUserRoles() {
		return this.studyUserRoles;
	}

	public void setStudyUserRoles(List<StudyUserRole> studyUserRoles) {
		this.studyUserRoles = studyUserRoles;
	}*/

	/*@OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccount")
	public List getDecisionConditions() {
		return this.decisionConditions;
	}

	public void setDecisionConditions(List decisionConditions) {
		this.decisionConditions = decisionConditions;
	}*/

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccount")
	public List<ItemData> getItemDatas() {
		return this.itemDatas;
	}

	public void setItemDatas(List<ItemData> itemDatas) {
		this.itemDatas = itemDatas;
	}

	/*@OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccount")
	public List getFilters() {
		return this.filters;
	}

	public void setFilters(List filters) {
		this.filters = filters;
	}*/

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccount")
	public List<Study> getStudies() {
		return this.studies;
	}

	public void setStudies(List<Study> studies) {
		this.studies = studies;
	}

	/*@OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccount")
	public List getDatasets() {
		return this.datasets;
	}

	public void setDatasets(List datasets) {
		this.datasets = datasets;
	}*/

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccount")
	public List<EventCrf> getEventCrfs() {
		return this.eventCrfs;
	}

	public void setEventCrfs(List<EventCrf> eventCrfs) {
		this.eventCrfs = eventCrfs;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccount")
	public List<StudyEvent> getStudyEvents() {
		return this.studyEvents;
	}

	public void setStudyEvents(List<StudyEvent> studyEvents) {
		this.studyEvents = studyEvents;
	}

	
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "userAccount")
	public List<CrfVersion> getCrfVersions() {
		return this.crfVersions;
	}

	public void setCrfVersions(List<CrfVersion> crfVersions) {
		this.crfVersions = crfVersions;
	}


	

}
