package com.example.blesdkdemo.txy.db;

import android.content.Context;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.List;

public class HistoryDao {
    private Dao<HistoryBean, Integer> historyDao;

    public HistoryDao(Context context) {
        try {
            this.historyDao = DBHelper.getInstance(context).getDao(HistoryBean.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int addHistory(HistoryBean historyBean) {
        try {
            return this.historyDao.create(historyBean);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int delHistoryById(int i) {
        try {
            return this.historyDao.deleteById(Integer.valueOf(i));
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public HistoryBean queryById(int i) {
        try {
            return this.historyDao.queryForId(Integer.valueOf(i));
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<HistoryBean> queryALLHistory() {
        try {
            return this.historyDao.queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}