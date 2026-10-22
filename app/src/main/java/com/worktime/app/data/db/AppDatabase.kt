package com.worktime.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DayRecordEntity::class,
        SegmentEntity::class,
        ExpenseCategoryEntity::class,
        ExpenseEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dayRecordDao(): DayRecordDao

    abstract fun expenseDao(): ExpenseDao

    abstract fun expenseCategoryDao(): ExpenseCategoryDao

    companion object {
        private const val NAME = "worktime.db"

        /** 首次建库时预置的分类。 */
        private val defaultCategories = listOf("餐饮", "交通", "购物", "其他")

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        defaultCategories.forEachIndexed { index, name ->
                            db.execSQL(
                                "INSERT INTO expense_category (name, sortOrder) VALUES (?, ?)",
                                arrayOf<Any>(name, index),
                            )
                        }
                    }
                })
                .build()
    }
}
