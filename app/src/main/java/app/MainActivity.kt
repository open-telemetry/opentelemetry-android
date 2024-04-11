package app

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.chuckerteam.chucker.api.Chucker
import com.example.hello_otel.R
import repo.TokenStore
import ui.LoggedInFragment
import ui.LoggedOutFragment

class MainActivity : AppCompatActivity(), LoggedInFragment.LoggedOutListener, LoggedOutFragment.LoggedInListener {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (TokenStore(AppContext.from(this)).isLoggedIn()) {
            bindLoggedInState()
        } else {
            bindLoggedOutState()

        }
    }

    private fun bindFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.root_fragment, fragment)
                .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_show_log -> showDialog()
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDialog(): Boolean {
        startActivity(Chucker.getLaunchIntent(this))
        return true
    }

    override fun onLoggedOut() {
        bindLoggedOutState()

    }

    override fun onLoggedIn() {
        bindLoggedInState()
    }

    private fun bindLoggedInState() {
        bindFragment(LoggedInFragment())
    }

    private fun bindLoggedOutState() {
        bindFragment(LoggedOutFragment())
    }

}