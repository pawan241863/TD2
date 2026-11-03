package gov.telangana.tdiagnostics

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import gov.telangana.tdiagnostics.databinding.ActivityMainBinding

/**
 * TD Admin - Telangana Diagnostics Official Mobile App.
 *
 * Integrated Shortcuts:
 * 1. 5 Predefined Accounts (tdadmin, gadnew, sidnew, newdb2, ntc)
 * 2. Patient Details Edit (UniqueDataReportNew.aspx)
 * 3. Delete / Update Aadhaar (UpdateDataReport.aspx)
 * 4. KPI Dashboard (Reporting/KPIDashBoard.aspx)
 * 5. Logout Session (logout.aspx)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView

    // Target URLs
    companion object {
        const val URL_LOGIN = "https://tdiagnostics.telangana.gov.in/"
        const val URL_PATIENT_EDIT = "https://tdiagnostics.telangana.gov.in/LabUser/UniqueDataReportNew.aspx"
        const val URL_DELETE_AADHAR = "https://tdiagnostics.telangana.gov.in/LabUser/UpdateDataReport.aspx"
        const val URL_KPI_DASHBOARD = "https://tdiagnostics.telangana.gov.in/Reporting/KPIDashBoard.aspx"
        const val URL_LOGOUT = "https://tdiagnostics.telangana.gov.in/logout.aspx"
    }

    // Pending autofill credentials (if user tapped one of the 5 accounts)
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

        // Initial Load
        loadUrl(URL_LOGIN)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView = binding.webView

        // Enable full web capabilities required by ASP.NET WebForms & AJAX
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

        // Ensure session cookies are persisted across requests
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        // Pull to refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
        }

        // WebView Client
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

                // Inject autofill if credentials are queued
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

        // WebChromeClient for Progress & Title
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
        // 1. 5 Quick Accounts Picker
        binding.btnQuickAccounts.setOnClickListener {
            showQuickAccountsBottomSheet()
        }

        // 2. Option 1: Patient Details Edit
        binding.btnNavPatientEdit.setOnClickListener {
            Toast.makeText(this, "Opening Patient Details Edit...", Toast.LENGTH_SHORT).show()
            loadUrl(URL_PATIENT_EDIT)
        }

        // 3. Option 2: Delete / Update Aadhaar
        binding.btnNavDeleteAadhar.setOnClickListener {
            Toast.makeText(this, "Opening Delete / Update Aadhaar...", Toast.LENGTH_SHORT).show()
            loadUrl(URL_DELETE_AADHAR)
        }

        // 4. Option 3: KPI Dashboard
        binding.btnNavKpiDashboard.setOnClickListener {
            Toast.makeText(this, "Opening KPI Dashboard...", Toast.LENGTH_SHORT).show()
            loadUrl(URL_KPI_DASHBOARD)
        }

        // 5. Logout Option
        binding.btnNavLoginHome.apply {
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
     * Bottom Sheet showing 5 Login Account Suggestions:
     * 1. tdadmin / abc1234 (Master Admin)
     * 2. gadnew / abc1234
     * 3. sidnew / abc1234
     * 4. newdb2 / abc1234
     * 5. ntc / lims123
     */
    private fun showQuickAccountsBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_login, null)
        dialog.setContentView(sheetView)

        // 1. tdadmin (Master Admin)
        sheetView.findViewById<Button?>(R.id.btnAutofillTdadmin)?.setOnClickListener {
            selectAccount("tdadmin", "abc1234")
            dialog.dismiss()
        }

        // 2. gadnew
        sheetView.findViewById<Button>(R.id.btnAutofillGadnew).setOnClickListener {
            selectAccount("gadnew", "abc1234")
            dialog.dismiss()
        }

        // 3. sidnew
        sheetView.findViewById<Button>(R.id.btnAutofillSidnew).setOnClickListener {
            selectAccount("sidnew", "abc1234")
            dialog.dismiss()
        }

        // 4. newdb2
        sheetView.findViewById<Button>(R.id.btnAutofillNewdb2).setOnClickListener {
            selectAccount("newdb2", "abc1234")
            dialog.dismiss()
        }

        // 5. ntc
        sheetView.findViewById<Button>(R.id.btnAutofillNtc).setOnClickListener {
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
                        userInput.style.backgroundColor = '#E8F5E9';
                        userInput.style.borderColor = '#2E7D32';
                    }

                    if (passInput) {
                        passInput.value = '$pass';
                        passInput.dispatchEvent(new Event('input', { bubbles: true }));
                        passInput.dispatchEvent(new Event('change', { bubbles: true }));
                        passInput.style.backgroundColor = '#E8F5E9';
                        passInput.style.borderColor = '#2E7D32';
                    }

                    var banner = document.getElementById('tdiagnostics-autofill-banner');
                    if (!banner) {
                        banner = document.createElement('div');
                        banner.id = 'tdiagnostics-autofill-banner';
                        banner.style.position = 'fixed';
                        banner.style.bottom = '15px';
                        banner.style.left = '50%';
                        banner.style.transform = 'translateX(-50%)';
                        banner.style.background = '#0B6E4F';
                        banner.style.color = '#FFFFFF';
                        banner.style.padding = '8px 16px';
                        banner.style.borderRadius = '20px';
                        banner.style.fontSize = '12px';
                        banner.style.fontWeight = 'bold';
                        banner.style.zIndex = '99999';
                        banner.style.boxShadow = '0 2px 8px rgba(0,0,0,0.3)';
                        document.body.appendChild(banner);
                    }
                    banner.innerText = '✓ Autofilled: $user';
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
        return when (item.itemId) {
            R.id.menu_quick_accounts -> {
                showQuickAccountsBottomSheet()
                true
            }
            R.id.menu_patient_details -> {
                loadUrl(URL_PATIENT_EDIT)
                true
            }
            R.id.menu_delete_aadhar -> {
                loadUrl(URL_DELETE_AADHAR)
                true
            }
            R.id.menu_kpi_dashboard -> {
                loadUrl(URL_KPI_DASHBOARD)
                true
            }
            R.id.menu_home_login -> {
                loadUrl(URL_LOGOUT)
                true
            }
            R.id.menu_refresh -> {
                webView.reload()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
