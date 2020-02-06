package com.example.top10downloader

import android.content.Context
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Exception
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.MalformedInputException
import kotlin.properties.Delegates

class FeedEntry {
    var name: String = ""
    var artist: String = ""
    var releaseDate: String = ""
    var summary: String = ""
    var imageURL: String = ""

    override fun toString(): String {
        return """
            name = $name
            arttist = $artist
            releaseDate = $releaseDate
            summary = $summary
            imageURL = $imageURL
        """.trimIndent()
    }
}

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private var downloadData: DownloadData? = null

    private var feedUrl: String = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml"
    private var feedLimit: Int = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //start async task
        downloadUrl(feedUrl.format(feedLimit))
        //end async task
    }

    private fun downloadUrl(feedUrl: String) {
        downloadData = DownloadData(this, xmlListView)
        downloadData?.execute(feedUrl)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.feeds_menu, menu)

        if (feedLimit == 10) {
            menu?.findItem(R.id.mnu10)?.isChecked = true
        } else {
            menu?.findItem(R.id.mnu25)?.isChecked = true
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.mnuFree ->
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml"
            R.id.mnuPaid ->
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/limit=%d/xml"
            R.id.mnuSongs ->
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml"
            R.id.mnu10, R.id.mnu25 -> {
                if (!item.isChecked) {
                    item.isChecked = true
                    feedLimit = 35 - feedLimit
                }
            }
            else ->
                return super.onOptionsItemSelected(item)
        }

        downloadUrl(feedUrl.format(feedLimit))
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadData?.cancel(true)
    }

    //start async task
    companion object {
        private class DownloadData(context: Context, listView: ListView) : AsyncTask<String, Void, String>() {
            private val TAG = "DownloadData"

            var propContext : Context by Delegates.notNull()
            var propListView : ListView by Delegates.notNull()

            init {
                propContext = context
                propListView = listView
            }

            //executes after doInBackground() returns data
            override fun onPostExecute(result: String) {
                super.onPostExecute(result)
                val parseApplications = ParseApplications()
                parseApplications.parse(result)
                Log.d(TAG, "onPostExecute return parameter is $result")

//                val arrayAdapter = ArrayAdapter<FeedEntry>(propContext, R.layout.list_item, parseApplications.applications)
//                propListView.adapter = arrayAdapter
                val feedAdapter = FeedAdapter(propContext, R.layout.list_record, parseApplications.applications)
                propListView.adapter = feedAdapter
            }

            //get data from downloadData.execute("U RL goes here")
            override fun doInBackground(vararg url: String?): String {
                Log.d(TAG, "doInBackground starts with ${url[0]}")
                val rssFeed = downloadXMLAuto(url[0])
                if (rssFeed.isEmpty()) {
                    Log.e(TAG, "doInBackground: Error downloading")
                }
                return rssFeed
            }

            //readText() does the trick automatically
            private fun downloadXMLAuto(urlPath: String?): String {
                return URL(urlPath).readText()
            }

            private fun downloadXMLManualMethod(urlPath: String?): String {
                val xmlResult =
                    StringBuilder() //StringBuilder is more efficient than concat string.

                try {
                    val url =
                        URL(urlPath) //URL checks if the passed string is a valid url, else throws an exception
                    val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                    val response = connection.responseCode
                    Log.d(TAG, "downloadXML: The response code was $response")

                    /*
                    //method: 1 (java)
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))

                    val inputBuffer = CharArray(500)
                    var charsRead = 0
                    while (charsRead >= 0) {
                        charsRead = reader.read(inputBuffer)
                        if(charsRead > 0) {
                            xmlResult.append(String(inputBuffer, 0, charsRead))
                        }
                    }

                    reader.close()
                    */

                    //replacement method: 2 (kotlin)
                    connection.inputStream.buffered().reader()
                        .use { xmlResult.append(it.readText()) }

                    Log.d(TAG, "Received ${xmlResult.length} bytes")
                    return xmlResult.toString()

                }
                /*
                    //method: 1 (java)
                catch (e: MalformedURLException) {
                    Log.e(TAG, "downloadXML: Invalid URL {$e.message}")
                } catch (e: IOException) {
                    Log.e(TAG, "downloadXML: IO Exception reading data {$e.message}")
                } catch (e: SecurityException) {
                    Log.e(TAG, "downloadXML: SecurityException. Needs permissions? {$e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "downloadXML: Unknown error {$e.message}")
                }
                */
                //replacement method: 2 (kotlin)
                catch (e: Exception) {
                    val errorMessage: String = when (e) {
                        is MalformedInputException -> "downloadXML: Invalid URL {$e.message}"
                        is IOException -> "downloadXML: IO Exception reading data {$e.message}"
                        is SecurityException -> {
                            e.printStackTrace()
                            "downloadXML: SecurityException. Needs permissions? {$e.message}"
                        }
                        else -> "Unknown error {$e.message}"
                    }
                }

                return "" //return an empty string if something goes wrong

            }
        }

    }
//end async task
}
