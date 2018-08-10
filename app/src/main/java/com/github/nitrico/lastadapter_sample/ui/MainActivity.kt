package com.github.nitrico.lastadapter_sample.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import android.view.Menu
import android.view.MenuItem
import com.github.nitrico.lastadapter_sample.data.Data
import com.github.nitrico.lastadapter_sample.R
import com.github.nitrico.lastadapter_sample.data.Header
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private val random = Random()

    private var randomPosition: Int = 0
        get() = random.nextInt(Data.items.size - 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        pager.adapter = ViewPagerAdapter(supportFragmentManager)
        tabs.setupWithViewPager(pager)
    }

    override fun onCreateOptionsMenu(menu: Menu) = consume { menuInflater.inflate(R.menu.main, menu) }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.addFirst -> consume {
            Data.items.add(0, Header("New Header"))
        }
        R.id.addLast -> consume {
            Data.items.add(Data.items.size, Header("New header"))
        }
        R.id.addRandom -> consume {
            Data.items.add(randomPosition, Header("New Header"))
        }
        R.id.removeFirst -> consume {
            if (Data.items.isNotEmpty()) Data.items.removeAt(0)
        }
        R.id.removeLast -> consume {
            if (Data.items.isNotEmpty()) Data.items.removeAt(Data.items.size - 1)
        }
        R.id.removeRandom -> consume {
            if (Data.items.isNotEmpty()) Data.items.removeAt(randomPosition)
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun consume(f: () -> Unit): Boolean {
        f()
        return true
    }

    class ViewPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        override fun getCount() = 3
        override fun getItem(i: Int) = when (i) {
            0 -> KotlinListFragment()
            1 -> JavaListFragment()
            2 -> KotlinPagedListFragment()
            else -> null
        }

        override fun getPageTitle(i: Int) = when (i) {
            0 -> "Kotlin"
            1 -> "Java"
            2 -> "Kotlin Paged"
            else -> null
        }
    }

}
