package sh.siava.pixelxpert.ui.misc;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import javax.inject.Singleton;

@Singleton
public class StateManager {

    private final MutableLiveData<Boolean> requiresSystemUIRestart = new MutableLiveData<>(false);

    public LiveData<Boolean> getRequiresSystemUIRestart() {
        return requiresSystemUIRestart;
    }

    public void setRequiresSystemUIRestart(boolean value) {
        requiresSystemUIRestart.postValue(value);
    }
}
