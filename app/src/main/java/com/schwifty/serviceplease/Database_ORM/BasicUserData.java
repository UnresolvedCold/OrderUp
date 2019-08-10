package com.schwifty.serviceplease.Database_ORM;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Property;
import org.greenrobot.greendao.annotation.Generated;

@Entity(nameInDb = "BasicUserData")
public class BasicUserData
{
    @Id(autoincrement = true)
    private Long Id;

    @Property(nameInDb = "email")
    private String email;

    @Generated(hash = 1426700129)
    public BasicUserData(Long Id, String email) {
        this.Id = Id;
        this.email = email;
    }

    @Generated(hash = 1230380909)
    public BasicUserData() {
    }

    public Long getId() {
        return this.Id;
    }

    public void setId(Long Id) {
        this.Id = Id;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}
