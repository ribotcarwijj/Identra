function before(hook, param)
    local fake = luajava.newInstance('java.lang.RuntimeException', 'privacy')
    param:setResult(fake)
    return true
end
