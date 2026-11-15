-- 0.7.7+: System.getProperty("os.version") = spoofed kernel release (same as Os.uname / uname / procfs)
function after(hook, param)
	local prop = param:getArgument(0)
	if prop ~= "os.version" then
		return false
	end
	local ret = param:getResult()
	local fake = param:getSetting("android.kernel.release")
	if fake == nil or fake == "" then
		return false
	end
	param:setResult(fake)
	return true, ret, fake
end
