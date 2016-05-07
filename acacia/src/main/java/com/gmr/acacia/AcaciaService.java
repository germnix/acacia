package com.gmr.acacia;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import com.gmr.acacia.impl.AcaciaFactory;
import com.gmr.acacia.impl.Constants;
import com.gmr.acacia.impl.ServiceExecutor;
import com.gmr.acacia.impl.ServiceThread;

import java.lang.reflect.Method;


/**
 * This is a generic implementation of Android {@link android.app.Service}.
 *
 * It is a bound service meant to be run locally in the same process as the application, so there's
 * no AIDL handling. Client implementations may extend this class in order to support multiple services
 * or have control over the service name published in AndroidManifest.xml.
 */
public class AcaciaService extends Service
{
    private ServiceExecutor serviceExecutor;


    /**
     * Class for clients to access. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder
    {
        public AcaciaService getService()
        {
            return AcaciaService.this;
        }
    }

    // This is the object that receives interactions from clients.
    private final IBinder binder = new LocalBinder();

    @Override
    public final IBinder onBind(Intent intent)
    {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(Constants.LOG_TAG, this.getClass().getSimpleName() + ": onStartCommand: " + intent);
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate()
    {
        Log.d(Constants.LOG_TAG, "Creating " + this.getClass().getSimpleName());
        super.onCreate();
    }

    @Override
    public void onDestroy()
    {
        Log.d(Constants.LOG_TAG, "Destroying " + this.getClass().getSimpleName());
        super.onDestroy();
    }


    /**
     * Create the instance of the class implementing the user defined service interface.
     *
     * @param aClass to instantiate.
     * @throws AcaciaException if the class cannot be instantiated.
     */
    @SuppressWarnings("unchecked")
    public final void setUserImplClass(Class<?> aClass) throws AcaciaException
    {
        if (serviceExecutor != null) { return; }

        try
        {
            Object userImpl;

            if (aClass.isInstance(this))
            {
                // User service implementation is a subclass of AcaciaService, so "this"
                userImpl = this;
            }
            else
            {
                // implementation is some other class
                userImpl = aClass.newInstance();
            }

            serviceExecutor = AcaciaFactory.newServiceExecutor(userImpl);

            if (userImpl instanceof ServiceAware)
            {
                ((ServiceAware) userImpl).setAndroidService(this);
            }
        }
        catch (InstantiationException | IllegalAccessException anEx)
        {
            throw new AcaciaException("Cannot instantiate " + aClass.getName() + ". Does it" +
                    " have an empty default constructor?", anEx);
        }
    }


    /**
     * Starts a worker thread for this service if it doesn't have one yet. Must be called after
     * the user implementation has been specified.
     */
    public final void startServiceThread()
    {
        serviceExecutor.startServiceThread();
    }


    /**
     * Invokes a generic method on the user supplied implementation using reflection.
     *
     * @param invokedMethod method to invoke.
     * @param args arguments to pass to the invoked method.
     * @return method invocation result.
     *
     * @throws Throwable if there's any error invoking the method.
     */
    public final Object invoke(Method invokedMethod, Object[] args) throws Throwable
    {
        return serviceExecutor.invoke(invokedMethod, args);
    }


    /**
     * Stops this service and its associated worker thread, if any.
     */
    public void stop()
    {
        stopSelf();
    }


    /**
     * Only used for tests.
     * @param aUserImpl to use as service implementation.
     */
    void setUserImpl(Object aUserImpl)
    {
        serviceExecutor = AcaciaFactory.newServiceExecutor(aUserImpl);
    }

    ServiceThread getServiceThread()
    {
        return serviceExecutor.getServiceThread();
    }
}
