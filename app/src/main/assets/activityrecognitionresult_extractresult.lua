function after(hook, param)
    local result = param:getResult()
    if result == nil then
        return false
    end

    local classActivity = luajava.bindClass('com.google.android.gms.location.DetectedActivity')
    local detected = luajava.new(classActivity, 4, 100) -- unknown, 100%
    local classResult = luajava.bindClass('com.google.android.gms.location.ActivityRecognitionResult')
    local time = result:getTime()
    local elapsed = result:getElapsedRealtimeMillis()
    local fake = luajava.new(classResult, detected, time, elapsed)
    param:setResult(fake)
    return true, result:toString(), fake:toString()
end
