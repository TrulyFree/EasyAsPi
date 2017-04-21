/*
 * EasyAsPi: A phone-based interface for the Raspberry Pi.
 * Copyright (C) 2017  vtcakavsmoace
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Raspberry Pi is a trademark of the Raspberry Pi Foundation.
 */

package io.github.trulyfree.easyaspi;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.google.gson.JsonParseException;

import java.io.IOException;

import io.github.trulyfree.easyaspi.lib.EAPActivity;
import io.github.trulyfree.easyaspi.lib.callback.StagedCallback;
import io.github.trulyfree.easyaspi.lib.disp.EAPDisplay;
import io.github.trulyfree.easyaspi.lib.dl.DownloadHandler;
import io.github.trulyfree.easyaspi.lib.io.FileHandler;
import io.github.trulyfree.easyaspi.lib.module.ModuleHandler;
import io.github.trulyfree.easyaspi.lib.module.conf.Config;
import io.github.trulyfree.easyaspi.lib.module.conf.ModuleConfig;

public class MainActivity extends EAPActivity {

    private DownloadHandler downloadHandler;
    private FileHandler fileHandler;
    private ModuleHandler moduleHandler;

    private int currentid = R.id.navigation_home;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            ViewSwitcher viewGroup = (ViewSwitcher) findViewById(R.id.content);
            int id = item.getItemId();
            if ((id == R.id.navigation_home || id == R.id.navigation_modules) && id != currentid) {
                currentid = item.getItemId();
                viewGroup.showNext();
                return true;
            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setup();
    }

    public boolean setup() {
        downloadHandler = new DownloadHandler(this);
        fileHandler = new FileHandler(this);
        moduleHandler = new ModuleHandler(this);

        moduleHandler.setup();

        setContentView(R.layout.activity_main);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        ViewSwitcher viewSwitcher = (ViewSwitcher) findViewById(R.id.content);
        Animation in = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        Animation out = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
        viewSwitcher.setInAnimation(in);
        viewSwitcher.setOutAnimation(out);

        Button getNewModule = (Button) findViewById(R.id.new_module_config_confirm);
        getNewModule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText editText = (EditText) findViewById(R.id.new_module_config_configurl);
                final String url = editText.getText().toString();
                Toast.makeText(MainActivity.this, "Requested config from: " + url, Toast.LENGTH_SHORT).show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ModuleConfig config;
                        try {
                            config = moduleHandler.getModuleConfig(url);
                        } catch (IOException e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Failed to get module config. :(", Toast.LENGTH_LONG).show();
                                }
                            });
                            return;
                        } catch (JsonParseException e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Config loaded was invalid. :(", Toast.LENGTH_LONG).show();
                                }
                            });
                            return;
                        }

                        final ModuleConfig finalconfig = config;
                        final ImageView imageView = (ImageView) findViewById(R.id.block_module_returned);
                        final int colorFrom = ContextCompat.getColor(MainActivity.this, R.color.colorFillingTint);
                        final int colorTo = ContextCompat.getColor(MainActivity.this, R.color.colorClear);
                        final ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                        colorAnimation.setDuration(100);
                        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(final ValueAnimator animator) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        imageView.setBackgroundColor((int) animator.getAnimatedValue());
                                    }
                                });
                            }
                        });
                        colorAnimation.addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animator) {
                            }

                            @Override
                            public void onAnimationEnd(final Animator animator) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final LinearLayout layout = (LinearLayout) findViewById(R.id.module_returned_config);
                                        final EditText moduleName = (EditText) findViewById(R.id.module_returned_configname);
                                        final EditText moduleVersion = (EditText) findViewById(R.id.module_returned_configversion);
                                        final EditText moduleConfigUrl = (EditText) findViewById(R.id.module_returned_configurl);
                                        final EditText moduleJarUrl = (EditText) findViewById(R.id.module_returned_jarurl);
                                        final LinearLayout moduleDependencies = (LinearLayout) findViewById(R.id.module_returned_dependencies);
                                        try {
                                            imageView.setVisibility(View.GONE);
                                            moduleName.setText(finalconfig.getName());
                                            moduleVersion.setText(finalconfig.getVersion());
                                            moduleConfigUrl.setText(finalconfig.getConfUrl());
                                            moduleJarUrl.setText(finalconfig.getJarUrl());
                                            moduleDependencies.removeAllViewsInLayout();
                                            for (Config dependency : finalconfig.getDependencies()) {
                                                LinearLayout child = (LinearLayout) ((LinearLayout) getLayoutInflater().inflate(R.layout.dependency, moduleDependencies)).getChildAt(0);
                                                EditText name = (EditText) child.getChildAt(0);
                                                name.setText(dependency.getName());
                                                EditText jarUrl = (EditText) child.getChildAt(1);
                                                jarUrl.setText(dependency.getJarUrl());
                                            }
                                            layout.setClickable(true);
                                            final Button validate = (Button) findViewById(R.id.module_returned_validate);
                                            validate.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    Toast.makeText(MainActivity.this, "Requesting jars...", Toast.LENGTH_SHORT).show();
                                                    new Thread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            final TextView stager = (TextView) findViewById(R.id.new_module_config_downloadstage);
                                                            final ProgressBar progressBar = (ProgressBar) findViewById(R.id.new_module_config_downloadprogress);
                                                            try {
                                                                moduleHandler.getNewModule(makeModuleCallback(stager, progressBar),
                                                                        finalconfig);
                                                            } catch (IOException e) {
                                                                e.printStackTrace();
                                                                runOnUiThread(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        stager.setText("");
                                                                        progressBar.setProgress(0);
                                                                    }
                                                                });
                                                            }
                                                        }
                                                    }).start();
                                                }
                                            });
                                            final Button cancel = (Button) findViewById(R.id.module_returned_cancel);
                                            cancel.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    Toast.makeText(MainActivity.this, "Cancelling request...", Toast.LENGTH_SHORT).show();
                                                    moduleName.setText(R.string.module_returned_configname);
                                                    moduleVersion.setText(R.string.module_returned_configversion);
                                                    moduleConfigUrl.setText(R.string.module_returned_configurl);
                                                    moduleJarUrl.setText(R.string.module_returned_jarurl);
                                                    moduleDependencies.removeAllViewsInLayout();
                                                    validate.setOnClickListener(null);
                                                    cancel.setOnClickListener(null);
                                                    final int colorFrom = ContextCompat.getColor(MainActivity.this, R.color.colorClear);
                                                    final int colorTo = ContextCompat.getColor(MainActivity.this, R.color.colorFillingTint);
                                                    final ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                                                    colorAnimation.setDuration(100);
                                                    colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                                        @Override
                                                        public void onAnimationUpdate(final ValueAnimator animator) {
                                                            runOnUiThread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    imageView.setBackgroundColor((int) animator.getAnimatedValue());
                                                                }
                                                            });
                                                        }
                                                    });
                                                    layout.setClickable(false);
                                                    imageView.setVisibility(View.VISIBLE);
                                                    colorAnimation.start();
                                                }
                                            });
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            Toast.makeText(MainActivity.this, "Module config invalid. :(", Toast.LENGTH_LONG).show();
                                            layout.setClickable(false);
                                            final int colorFrom = ContextCompat.getColor(MainActivity.this, R.color.colorClear);
                                            final int colorTo = ContextCompat.getColor(MainActivity.this, R.color.colorFillingTint);
                                            final ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                                            colorAnimation.setDuration(500);
                                            colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                                @Override
                                                public void onAnimationUpdate(ValueAnimator animator) {
                                                    imageView.setBackgroundColor((int) animator.getAnimatedValue());
                                                }
                                            });
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onAnimationCancel(Animator animator) {
                            }

                            @Override
                            public void onAnimationRepeat(Animator animator) {
                            }
                        });
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                colorAnimation.start();
                            }
                        });
                    }
                }).start();
            }
        });

        refreshFilling();
        return true;
    }

    private void refreshFilling() {
        LinearLayout dashboard = (LinearLayout) findViewById(R.id.dashboard);
        dashboard.removeAllViewsInLayout();
        if (moduleHandler.getConfigs().length != 0) {
            LinearLayout moduleList = (LinearLayout) ((LinearLayout) getLayoutInflater().inflate(R.layout.modulelist, dashboard)).getChildAt(0);
            Button refreshAll = (Button) findViewById(R.id.refresh_all);
            refreshAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(MainActivity.this, "Refreshing jars...", Toast.LENGTH_SHORT).show();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final TextView stager = (TextView) findViewById(R.id.refresh_download_stage);
                            final ProgressBar progressBar = (ProgressBar) findViewById(R.id.refresh_bar);
                            try {
                                moduleHandler.refreshAll(makeModuleCallback(stager, progressBar));
                            } catch (IOException e) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "Refresh failed. :(", Toast.LENGTH_SHORT).show();
                                        stager.setText("");
                                        progressBar.setProgress(0);
                                    }
                                });
                            }
                        }
                    }).start();
                }
            });
            LinearLayout scrolledModuleList = (LinearLayout) ((ScrollView) moduleList.getChildAt(3)).getChildAt(0);
            for (int i = 0; i < moduleHandler.getConfigs().length; i++) {
                final int intermediary = i;
                LinearLayout layout = (LinearLayout) ((LinearLayout) getLayoutInflater().inflate(R.layout.module, scrolledModuleList)).getChildAt(i);
                EditText moduleName = (EditText) layout.getChildAt(0);
                moduleName.setText(moduleHandler.getConfigs()[i].getName());
                Button launcher = (Button) layout.getChildAt(1);
                launcher.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent myIntent = new Intent(MainActivity.this, EAPDisplay.class);
                        myIntent.putExtra("targetModule", moduleHandler.toJson(moduleHandler.getConfigs()[intermediary]));
                        MainActivity.this.startActivityForResult(myIntent, intermediary);
                    }
                });
                Button delete = (Button) layout.getChildAt(2);
                delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final ModuleConfig config = moduleHandler.getConfigs()[intermediary];
                        boolean success = false;
                        try {
                            success = moduleHandler.remove(null, config);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        final String toast = "Deletion of " + config.getName() + " was " +
                                ((success) ? "successful." : "unsuccessful.");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();
                                refreshFilling();
                            }
                        });
                    }
                });
            }
        } else {
            getLayoutInflater().inflate(R.layout.no_module_modulelist, dashboard);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED && data != null) {
            Bundle bundle = data.getExtras();
            Toast.makeText(this,
                    moduleHandler.getConfigs()[requestCode].getName() + " crashed: " + bundle.getString("error"),
                    Toast.LENGTH_LONG).show();
        } else if (resultCode == RESULT_OK || data == null) {
            Toast.makeText(this,
                    moduleHandler.getConfigs()[requestCode].getName() + " finished cleanly. :)",
                    Toast.LENGTH_LONG).show();
        }
    }

    private StagedCallback makeModuleCallback(final TextView stageText, final ProgressBar progressBar) {
        return new StagedCallback() {
            private String[] names;
            private int stage = 0;

            @Override
            public void setStages(String[] names) {
                this.names = names;
            }

            @Override
            public void onStart() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stageText.setText(names[stage]);
                    }
                });
            }

            @Override
            public void onProgress(final int current) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int numerator = stage * 100 + current;
                        int denominator = names.length;
                        final int progress = numerator / denominator;
                        progressBar.setProgress(progress);
                    }
                });
            }

            @Override
            public void onFinish() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stage++;
                        if (stage == names.length) {
                            stageText.setText("");
                            progressBar.setProgress(0);
                            Toast.makeText(MainActivity.this, "Download(s) successful.", Toast.LENGTH_SHORT).show();
                            refreshFilling();
                        }
                    }
                });
            }
        };
    }

    public DownloadHandler getDownloadHandler() {
        return downloadHandler;
    }

    public FileHandler getFileHandler() {
        return fileHandler;
    }

    public ModuleHandler getModuleHandler() {
        return moduleHandler;
    }
}
