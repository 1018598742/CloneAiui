package com.fta.menlo.db;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.fta.menlo.chat.RawMessage;

import java.util.List;

@Dao
public interface MessageDao {
    @Insert
    void addMessage(RawMessage msg);

    @Update
    void updateMessage(RawMessage msg);

    @Query("SELECT * from RAWMESSAGE")
    LiveData<List<RawMessage>> getAllMessage();
}
