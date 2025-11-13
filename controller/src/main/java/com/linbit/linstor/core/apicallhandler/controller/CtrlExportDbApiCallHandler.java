package com.linbit.linstor.core.apicallhandler.controller;

import com.linbit.linstor.api.ApiCallRc;
import com.linbit.linstor.api.ApiCallRcImpl;
import com.linbit.linstor.api.ApiConsts;
import com.linbit.linstor.core.apicallhandler.ScopeRunner;
import com.linbit.linstor.core.apicallhandler.controller.db.DbExportImportHelper;
import com.linbit.linstor.core.apicallhandler.response.ApiRcException;
import com.linbit.linstor.logging.ErrorReporter;
import com.linbit.locks.LockGuardFactory;
import com.linbit.locks.LockGuardFactory.LockObj;
import com.linbit.locks.LockGuardFactory.LockType;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import reactor.core.publisher.Flux;

@Singleton
public class CtrlExportDbApiCallHandler
{
    private final ErrorReporter errorReporter;
    private final ScopeRunner scopeRunner;
    private final LockGuardFactory lockGuardFactory;
    private final DbExportImportHelper dbExportImportHelper;

    @Inject
    public CtrlExportDbApiCallHandler(
        ErrorReporter errorReporterRef,
        ScopeRunner scopeRunnerRef,
        LockGuardFactory lockGuardFactoryRef,
        DbExportImportHelper dbExportImportHelperRef
    )
    {
        errorReporter = errorReporterRef;
        scopeRunner = scopeRunnerRef;
        lockGuardFactory = lockGuardFactoryRef;
        dbExportImportHelper = dbExportImportHelperRef;
    }

    public Flux<ApiCallRc> exportDatabase(String targetPathStr)
    {
        try
        {
            return exportDatabase(Paths.get(targetPathStr));
        }
        catch (InvalidPathException ipe)
        {
            throw new ApiRcException(
                ApiCallRcImpl.entryBuilder(
                    ApiConsts.FAIL_INVLD_DB_EXPORT_FILE,
                    "The parameter '" + targetPathStr + "' is not a valid path"
                )
                    .setSkipErrorReport(true)
                    .build(),
                ipe
            );
        }
    }

    public Flux<ApiCallRc> exportDatabase(Path targetPath)
    {
        return scopeRunner.fluxInTransactionalScope(
            "Exporting database",
            lockGuardFactory.buildDeferred(LockType.WRITE, LockObj.RECONFIGURATION),
            () -> exportDatabaseInTx(targetPath)
        );
    }

    private Flux<ApiCallRc> exportDatabaseInTx(Path targetPathRef)
    {
        dbExportImportHelper.export(targetPathRef);
        return Flux.<ApiCallRc> just(
            ApiCallRcImpl.singleApiCallRc(ApiConsts.MASK_SUCCESS, "Exported database to " + targetPathRef)
        );
    }
}
