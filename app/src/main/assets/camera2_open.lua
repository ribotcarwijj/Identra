function before(hook, param)
    local exception = luajava.bindClass('android.hardware.camera2.CameraAccessException')
    local fake = luajava.new(exception, 1, 'privacy') -- 1=disabled
    param:setResult(fake)
    return true
end
