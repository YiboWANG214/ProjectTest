package projecttest.libraryManagement;

import java.sql.Date;


/**
 *
 * @author testuser
 */
public class Member {

    private Integer memberId;
    private String memberName;
    private Date dateOfJoining;

    public Member() {

    }

    public Member(Integer memberId, String memberName, Date dateOfJoining) {
        this.memberId = memberId;
        this.memberName = memberName;
        this.dateOfJoining = dateOfJoining;
    }

    public Date getDateOfJoining() {
        return dateOfJoining;
    }

    public void setDateOfJoining(Date dateOfJoining) {
        this.dateOfJoining = dateOfJoining;
    }

    public Integer getMemberId() {
        return memberId;
    }

    public void setMemberId(Integer memberId) {
        this.memberId = memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

}
