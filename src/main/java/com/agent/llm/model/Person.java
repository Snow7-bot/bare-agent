package com.agent.llm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Person {
    private String name;
    private Integer age;
    private String phone;
    private String email;

    @JsonProperty("skills")
    private String skills;

    public Person() {}

    // ====== Getter / Setter ======
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    @Override
    public String toString() {
        return "Person{name='" + name + "', age=" + age
                + ", phone='" + phone + "', email='" + email
                + "', skills='" + skills + "'}";
    }
}