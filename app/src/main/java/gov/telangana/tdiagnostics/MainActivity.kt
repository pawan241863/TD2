package gov.telangana.tdiagnostics

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import gov.telangana.tdiagnostics.databinding.ActivityMainBinding

/**
 * TD Admin - Telangana Diagnostics Official Mobile App.
 *
 * Fully resilient implementation:
 * - Dynamically binds to both new and legacy XML layout IDs
 * - Zero compile-time errors even if activity_main.xml is not yet updated
 * - Direct access to:
 *   1. 5 Predefined Accounts (tdadmin, gadnew, sidnew, newdb2, ntc)
 *   2. Patient Details Edit (UniqueDataReportNew.aspx)
 *   3. Delete / Update Aadhaar (UpdateDataReport.aspx)
 *   4. KPI Dashboard (Reporting/KPIDashBoard.aspx)
 *   5. Logout Session (logout.aspx)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView

    companion object {
        const val URL_LOGIN = "https://tdiagnostics.telangana.gov.in/"
        const val URL_PATIENT_EDIT = "https://tdiagnostics.telangana.gov.in/LabUser/UniqueDataReportNew.aspx"
        const val URL_DELETE_AADHAR = "https://tdiagnostics.telangana.gov.in/LabUser/UpdateDataReport.aspx"
        const val URL_KPI_DASHBOARD = "https://tdiagnostics.telangana.gov.in/Reporting/KPIDashBoard.aspx"
        const val URL_LOGOUT = "https://tdiagnostics.telangana.gov.in/logout.aspx"
    }

    // Pending autofill credentials
    private var pendingUser: String? = null
    private var pendingPass: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupWebView()
        setupNavigationButtons()
        setupBackNavigation()

        // Initial Load: starts on Login portal
        loadUrl(URL_LOGIN)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView = binding.webView

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.setSupportZoom(true)
        settings.allowFileAccess = true
        settings.allowContentAccess = true

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        binding.swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.pageProgressBar.visibility = View.VISIBLE
                binding.errorView.visibility = View.GONE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.pageProgressBar.visibility = View.GONE
                binding.swipeRefreshLayout.isRefreshing = false

                injectAutofillScript(view, pendingUser, pendingPass)
                pendingUser = null
                pendingPass = null
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                binding.pageProgressBar.visibility = View.GONE
                binding.swipeRefreshLayout.isRefreshing = false
                binding.errorView.visibility = View.VISIBLE
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.pageProgressBar.progress = newProgress
                if (newProgress == 100) {
                    binding.pageProgressBar.visibility = View.GONE
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                if (!title.isNullOrEmpty() && !title.contains("http", ignoreCase = true)) {
                    binding.toolbar.subtitle = title
                }
            }
        }

        binding.btnRetry.setOnClickListener {
            binding.errorView.visibility = View.GONE
            webView.reload()
        }
    }

    private fun setupNavigationButtons() {
        // 1. Accounts Picker (Long press triggers KPI Dashboard directly!)
        binding.btnQuickAccounts.setOnClickListener {
            showQuickAccountsBottomSheet()
        }
        binding.btnQuickAccounts.setOnLongClickListener {
            Toast.makeText(this, "Opening KPI Dashboard...", Toast.LENGTH_SHORT).show()
            loadUrl(URL_KPI_DASHBOARD)
            true
        }

        // 2. Patient Details Edit
        binding.btnNavPatientEdit.setOnClickListener {
            Toast.makeText(this, "Opening Patient Details Edit...", Toast.LENGTH_SHORT).show()
            loadUrl(URL_PATIENT_EDIT)
        }

        // 3. Delete / Update Aadhaar
        binding.btnNavDeleteAadhar.setOnClickListener {
            Toast.makeText(this, "Opening Delete / Update Aadhaar...", Toast.LENGTH_SHORT).show()
            loadUrl(URL_DELETE_AADHAR)
        }

        // 4. Dynamic KPI Button Binding (Works if btnNavKpiDashboard exists in XML, no compile error if not!)
        val kpiBtnId = resources.getIdentifier("btnNavKpiDashboard", "id", packageName)
        if (kpiBtnId != 0) {
            findViewById<View>(kpiBtnId)?.setOnClickListener {
                Toast.makeText(this, "Opening KPI Dashboard...", Toast.LENGTH_SHORT).show()
                loadUrl(URL_KPI_DASHBOARD)
            }
        }

        // 5. Logout Action: Replaces login button in existing layout
        val logoutBtnId = resources.getIdentifier("btnNavLogout", "id", packageName)
        val loginBtnId = resources.getIdentifier("btnNavLoginHome", "id", packageName)

        val targetLogoutView = if (logoutBtnId != 0) findViewById<Button>(logoutBtnId) else null
        val targetLoginView = if (loginBtnId != 0) findViewById<Button>(loginBtnId) else null

        val activeLogoutButton = targetLogoutView ?: targetLoginView
        activeLogoutButton?.apply {
            text = "🚪 Exit"
            setOnClickListener {
                Toast.makeText(this@MainActivity, "Logging out of session...", Toast.LENGTH_SHORT).show()
                loadUrl(URL_LOGOUT)
            }
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun loadUrl(url: String) {
        binding.errorView.visibility = View.GONE
        webView.loadUrl(url)
    }

    /**
     * Account Switcher Bottom Sheet:
     * - Shows all 5 accounts (tdadmin, gadnew, sidnew, newdb2, ntc)
     * - Includes direct action button for KPI Dashboard
     */
    private fun showQuickAccountsBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_login, null)
        dialog.setContentView(sheetView)

        // 1. tdadmin (Master Admin / KPI Login)
        val btnTdadmin = sheetView.findViewById<Button?>(resources.getIdentifier("btnAutofillTdadmin", "id", packageName))
        btnTdadmin?.setOnClickListener {
            selectAccount("tdadmin", "abc1234")
            dialog.dismiss()
        }

        // Fallback: If tdadmin is not in older XML, attach click to first card or show quick picker
        val cardTdadmin = sheetView.findViewById<View?>(resources.getIdentifier("cardAccountTdadmin", "id", packageName))
        cardTdadmin?.setOnClickListener {
            selectAccount("tdadmin", "abc1234")
            dialog.dismiss()
        }

        // 2. gadnew
        val btnGadnew = sheetView.findViewById<Button?>(resources.getIdentifier("btnAutofillGadnew", "id", packageName))
        btnGadnew?.setOnClickListener {
            selectAccount("gadnew", "abc1234")
            dialog.dismiss()
        }

        // 3. sidnew
        val btnSidnew = sheetView.findViewById<Button?>(resources.getIdentifier("btnAutofillSidnew", "id", packageName))
        btnSidnew?.setOnClickListener {
            selectAccount("sidnew", "abc1234")
            dialog.dismiss()
        }

        // 4. newdb2
        val btnNewdb2 = sheetView.findViewById<Button?>(resources.getIdentifier("btnAutofillNewdb2", "id", packageName))
        btnNewdb2?.setOnClickListener {
            selectAccount("newdb2", "abc1234")
            dialog.dismiss()
        }

        // 5. ntc
        val btnNtc = sheetView.findViewById<Button?>(resources.getIdentifier("btnAutofillNtc", "id", packageName))
        btnNtc?.setOnClickListener {
            selectAccount("ntc", "lims123")
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun selectAccount(username: String, pass: String) {
        Toast.makeText(this, "Selected: $username", Toast.LENGTH_SHORT).show()
        val currentUrl = webView.url ?: ""

        if (!currentUrl.contains("tdiagnostics.telangana.gov.in") || currentUrl.contains("LabUser") || currentUrl.contains("Reporting")) {
            pendingUser = username
            pendingPass = pass
            loadUrl(URL_LOGIN)
        } else {
            injectAutofillScript(webView, username, pass)
        }
    }

    /**
     * Robust JavaScript autofill injector tailored for ASP.NET WebForms.
     */
    private fun injectAutofillScript(view: WebView?, user: String?, pass: String?) {
        if (view == null || user == null || pass == null) return

        val script = """
            (function() {
                try {
                    var userInputs = document.querySelectorAll(
                        "input[id*='txtUserName'], input[id*='txtUsername'], input[id*='txtUser'], " +
                        "input[id*='txtLogin'], input[id*='txtUserId'], input[name*='User'], " +
                        "input[type='text']:not([style*='display: none'])"
                    );
                    var userInput = userInputs.length > 0 ? userInputs[0] : null;
                    var passInput = document.querySelector("input[type='password']");

                    if (userInput) {
                        userInput.value = '$user';
                        userInput.dispatchEvent(new Event('input', { bubbles: true }));
                        userInput.dispatchEvent(new Event('change', { bubbles: true }));
                        userInput.style.backgroundColor = '#FEF3C7';
                        userInput.style.borderColor = '#F59E0B';
                    }

                    if (passInput) {
                        passInput.value = '$pass';
                        passInput.dispatchEvent(new Event('input', { bubbles: true }));
                        passInput.dispatchEvent(new Event('change', { bubbles: true }));
                        passInput.style.backgroundColor = '#FEF3C7';
                        passInput.style.borderColor = '#F59E0B';
                    }

                    var banner = document.getElementById('tdiagnostics-autofill-banner');
                    if (!banner) {
                        banner = document.createElement('div');
                        banner.id = 'tdiagnostics-autofill-banner';
                        banner.style.position = 'fixed';
                        banner.style.bottom = '15px';
                        banner.style.left = '50%';
                        banner.style.transform = 'translateX(-50%)';
                        banner.style.background = '#064E3B';
                        banner.style.color = '#FDE68A';
                        banner.style.border = '1px solid #F59E0B';
                        banner.style.padding = '8px 18px';
                        banner.style.borderRadius = '24px';
                        banner.style.fontSize = '12px';
                        banner.style.fontWeight = 'bold';
                        banner.style.zIndex = '99999';
                        banner.style.boxShadow = '0 4px 12px rgba(0,0,0,0.35)';
                        document.body.appendChild(banner);
                    }
                    banner.innerText = '🛡️ TD Admin: Autofilled $user';
                    setTimeout(function() { banner.style.display = 'none'; }, 3000);
                } catch(e) {
                    console.log('Autofill error: ' + e);
                }
            })();
        """.trimIndent()

        view.evaluateJavascript(script, null)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Dynamic check for KPI Dashboard menu item
        val kpiMenuId = resources.getIdentifier("menu_kpi_dashboard", "id", packageName)
        if (kpiMenuId != 0 && item.itemId == kpiMenuId) {
            Toast.makeText(this, "Opening KPI Dashboard...", Toast.LENGTH_SHORT).show()
            loadUrl(URL_KPI_DASHBOARD)
            return true
        }

        // Standard actions
        val accountsMenuId = resources.getIdentifier("menu_quick_accounts", "id", packageName)
        val patientMenuId = resources.getIdentifier("menu_patient_details", "id", packageName)
        val aadharMenuId = resources.getIdentifier("menu_delete_aadhar", "id", packageName)
        val loginMenuId = resources.getIdentifier("menu_home_login", "id", packageName)
        val logoutMenuId = resources.getIdentifier("menu_logout", "id", packageName)
        val refreshMenuId = resources.getIdentifier("menu_refresh", "id", packageName)

        return when (item.itemId) {
            accountsMenuId -> {
                showQuickAccountsBottomSheet()
                true
            }
            patientMenuId -> {
                loadUrl(URL_PATIENT_EDIT)
                true
            }
            aadharMenuId -> {
                loadUrl(URL_DELETE_AADHAR)
                true
            }
            loginMenuId, logoutMenuId -> {
                loadUrl(URL_LOGOUT)
                true
            }
            refreshMenuId -> {
                webView.reload()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
