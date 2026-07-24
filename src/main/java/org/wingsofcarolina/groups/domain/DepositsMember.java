package org.wingsofcarolina.groups.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import org.bson.Document;
import org.bson.types.ObjectId;

public class DepositsMember {

  @JsonIgnore
  private ObjectId objectId;

  private String firstName;
  private String lastName;
  private String email;
  private String memberNumber;
  private String fullNameNormalized;
  private String numberNormalized;
  private Boolean inactive;
  private Instant createdAt;
  private Boolean checked;

  public DepositsMember() {
    this.checked = false;
  }

  public static DepositsMember fromGroupsMember(Member member) {
    DepositsMember depositsMember = new DepositsMember();
    depositsMember.setFirstName(member.getFirstName());
    depositsMember.setLastName(member.getLastName());
    depositsMember.setEmail(member.getEmail());
    depositsMember.setMemberNumber(String.valueOf(member.getId()));
    depositsMember.setInactive(false);
    depositsMember.prepareForInsert();
    return depositsMember;
  }

  public static DepositsMember fromDocument(Document document) {
    DepositsMember member = new DepositsMember();
    member.setObjectId(document.getObjectId("_id"));
    member.setFirstName(document.getString("first_name"));
    member.setLastName(document.getString("last_name"));
    member.setEmail(document.getString("email"));
    member.setMemberNumber(document.getString("member_number"));
    member.setFullNameNormalized(
      normalizeFullName(member.getFirstName(), member.getLastName())
    );
    member.setNumberNormalized(normalizeMemberNumber(member.getMemberNumber()));
    member.setInactive(Boolean.TRUE.equals(document.getBoolean("inactive")));
    Date createdAt = document.getDate("created_at");
    if (createdAt != null) {
      member.setCreatedAt(createdAt.toInstant());
    }
    return member;
  }

  public void prepareForInsert() {
    firstName = trim(firstName);
    lastName = trim(lastName);
    email = trim(email);
    memberNumber = trim(memberNumber);
    fullNameNormalized = normalizeFullName(firstName, lastName);
    numberNormalized = normalizeMemberNumber(memberNumber);
    inactive = Boolean.TRUE.equals(inactive);
    createdAt = Instant.now();
  }

  public Document toDocument() {
    return new Document()
      .append("first_name", firstName)
      .append("last_name", lastName)
      .append("email", email)
      .append("member_number", memberNumber)
      .append("full_name_normalized", fullNameNormalized)
      .append("number_normalized", numberNormalized)
      .append("inactive", Boolean.TRUE.equals(inactive))
      .append("created_at", Date.from(createdAt));
  }

  public boolean matches(DepositsMember other) {
    return (
      equals(firstName, other.firstName) &&
      equals(lastName, other.lastName) &&
      equals(fullNameNormalized, other.fullNameNormalized) &&
      equals(email, other.email) &&
      equals(numberNormalized, other.numberNormalized) &&
      Boolean.TRUE.equals(inactive) == Boolean.TRUE.equals(other.inactive)
    );
  }

  public String displayName() {
    return trim(firstName + " " + lastName);
  }

  public static String normalizeFullName(String firstName, String lastName) {
    return String
      .join(" ", trim(firstName + " " + lastName).toLowerCase(Locale.ROOT).split("\\s+"))
      .trim();
  }

  public static String normalizeEmail(String email) {
    return trim(email).toLowerCase(Locale.ROOT);
  }

  public static String normalizeMemberNumber(String number) {
    String trimmed = trim(number);
    if (trimmed.matches("\\d+")) {
      return new BigInteger(trimmed).toString();
    }
    return trimmed.toLowerCase(Locale.ROOT);
  }

  private static boolean equals(String a, String b) {
    return trim(a).equals(trim(b));
  }

  private static String trim(String value) {
    if (value == null) {
      return "";
    }
    return value.trim();
  }

  public ObjectId getObjectId() {
    return objectId;
  }

  public void setObjectId(ObjectId objectId) {
    this.objectId = objectId;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getMemberNumber() {
    return memberNumber;
  }

  public void setMemberNumber(String memberNumber) {
    this.memberNumber = memberNumber;
  }

  public String getFullNameNormalized() {
    return fullNameNormalized;
  }

  public void setFullNameNormalized(String fullNameNormalized) {
    this.fullNameNormalized = fullNameNormalized;
  }

  public String getNumberNormalized() {
    return numberNormalized;
  }

  public void setNumberNormalized(String numberNormalized) {
    this.numberNormalized = numberNormalized;
  }

  public Boolean getInactive() {
    return inactive;
  }

  public void setInactive(Boolean inactive) {
    this.inactive = inactive;
  }

  @JsonIgnore
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Boolean getChecked() {
    return checked;
  }

  public void setChecked(Boolean checked) {
    this.checked = checked;
  }
}
